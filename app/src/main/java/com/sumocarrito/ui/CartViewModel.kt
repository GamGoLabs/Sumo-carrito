package com.sumocarrito.ui
 
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumocarrito.billing.BillingManager
import com.sumocarrito.data.local.PurchaseDao
import com.sumocarrito.data.local.PurchaseEntity
import com.sumocarrito.data.prefs.SettingsStore
import com.sumocarrito.data.rate.BcvCertException
import com.sumocarrito.data.rate.BcvRateService
import com.sumocarrito.data.repo.CartRepository
import com.sumocarrito.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import javax.inject.Inject
 
data class CartUiState(
    val lines: List<CartLine> = emptyList(),
    val totals: CartTotals? = null,
    val budget: BigDecimal = BigDecimal.ZERO,
    val rate: BigDecimal = BigDecimal.ONE,
    val payment: PaymentSelection = PaymentSelection.CashUsd(),
    val config: TaxConfig = TaxConfig()
)
 
data class RateStatus(val label: String = "", val loading: Boolean = false, val error: String? = null, val needsConsent: Boolean = false)
 
sealed interface UiEvent { data object BudgetExceeded : UiEvent }
 
@HiltViewModel
class CartViewModel @Inject constructor(
    private val repo: CartRepository,
    private val engine: TaxCalculatorEngine,
    private val prefs: SettingsStore,
    private val purchases: PurchaseDao,
    private val bcv: BcvRateService,
    val billing: BillingManager
) : ViewModel() {
 
    private val budget = MutableStateFlow(InputSanitizer.parseDecimal(prefs.budget) ?: BigDecimal.ZERO)
    private val rate = MutableStateFlow(InputSanitizer.parseDecimal(prefs.rate) ?: BigDecimal.ONE)
    private val payment = MutableStateFlow<PaymentSelection>(PaymentSelection.CashUsd())
    private val config = MutableStateFlow(TaxConfig(
        vatRate = InputSanitizer.parseDecimal(prefs.vatRate) ?: BigDecimal("0.16"),
        igtfRate = InputSanitizer.parseDecimal(prefs.igtfRate) ?: BigDecimal("0.03"),
        pricesIncludeVat = prefs.pricesIncludeVat))
 
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
 
    val isPro = billing.isPro
 
    private val _highContrast = MutableStateFlow(prefs.highContrast)
    val highContrast: StateFlow<Boolean> = _highContrast
    fun setHighContrast(v: Boolean) { _highContrast.value = v; prefs.highContrast = v }
 
    private val _rateStatus = MutableStateFlow(RateStatus(prefs.rateDate))
    val rateStatus: StateFlow<RateStatus> = _rateStatus
    /** Sube solo cuando la tasa cambia por consulta al BCV, para refrescar el campo sin pisar lo que se escribe. */
    private val _rateTick = MutableStateFlow(0)
    val rateTick: StateFlow<Int> = _rateTick
 
    /** [userConsent] = el usuario pulsó "Obtener de todos modos": autoriza (y reinicia) la huella fijada del BCV. */
    fun refreshRate(userConsent: Boolean = false) {
        if (_rateStatus.value.loading) return
        if (userConsent) { prefs.bcvLenient = true; prefs.bcvPin = null }
        viewModelScope.launch {
            _rateStatus.value = _rateStatus.value.copy(loading = true, error = null, needsConsent = false)
            bcv.fetch(lenient = prefs.bcvLenient).onSuccess {
                if (!BcvRateParser.plausible(it.rate, rate.value)) {
                    _rateStatus.value = RateStatus(prefs.rateDate, error = "La tasa recibida (${it.rate.toPlainString()}) parece inválida; se conserva la anterior.")
                } else {
                    rate.value = it.rate; prefs.rate = it.rate.toPlainString()
                    prefs.rateDate = it.dateLabel ?: ""
                    _rateStatus.value = RateStatus(it.dateLabel ?: "")
                    _rateTick.value += 1
                }
            }.onFailure {
                _rateStatus.value = RateStatus(prefs.rateDate, error = it.message, needsConsent = it is BcvCertException)
            }
        }
    }
 
    val history: StateFlow<List<PurchaseEntity>> = purchases.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
 
    val uiState: StateFlow<CartUiState> = combine(
        repo.observeLines(), budget, rate, payment, config
    ) { lines, b, r, p, c ->
        CartUiState(lines, engine.calculate(lines, c, r, p, b), b, r, p, c)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState())
 
    init {
        refreshRate()   // intento silencioso al abrir; si no hay red, se usa la última tasa guardada
        // Vibración solo en la transición a ROJO.
        viewModelScope.launch {
            uiState.map { it.totals?.budgetStatus }.distinctUntilChanged()
                .collect { if (it == BudgetStatus.RED) _events.send(UiEvent.BudgetExceeded) }
        }
    }
 
    fun setBudget(raw: String) {
        val v = InputSanitizer.parseDecimal(raw) ?: BigDecimal.ZERO
        budget.value = v; prefs.budget = v.toPlainString()
    }
    fun setRate(raw: String) {
        InputSanitizer.parseDecimal(raw)?.takeIf { it.signum() > 0 }?.let { rate.value = it; prefs.rate = it.toPlainString() }
    }
    fun setPayment(p: PaymentSelection) { payment.value = p }
    fun setConfig(c: TaxConfig) {
        config.value = c; prefs.vatRate = c.vatRate.toPlainString()
        prefs.igtfRate = c.igtfRate.toPlainString(); prefs.pricesIncludeVat = c.pricesIncludeVat
    }
 
    fun addItem(name: String, priceRaw: String, qtyRaw: String, exempt: Boolean) {
        val price = InputSanitizer.parseDecimal(priceRaw) ?: return
        val qty = InputSanitizer.parseDecimal(qtyRaw)?.takeIf { it.signum() > 0 } ?: BigDecimal.ONE
        viewModelScope.launch { repo.add(name, price, qty, exempt) }
    }
    fun toggleExempt(l: CartLine) = viewModelScope.launch { repo.setExempt(l.id, !l.vatExempt) }
    fun removeItem(id: Long) = viewModelScope.launch { repo.remove(id) }
 
    /** Finaliza: si es Pro guarda en historial; luego limpia el carrito. */
    fun finishPurchase(store: String, onDone: () -> Unit) = viewModelScope.launch {
        val s = uiState.value
        val t = s.totals
        if (isPro.value && t != null && s.lines.isNotEmpty()) {
            val json = JSONArray(s.lines.map {
                JSONObject().put("n", it.name).put("p", it.unitPrice.toPlainString())
                    .put("q", it.quantity.toPlainString()).put("e", it.vatExempt)
            }).toString()
            purchases.insert(PurchaseEntity(epochMillis = System.currentTimeMillis(),
                store = store.ifBlank { "Sin nombre" }, totalUsd = t.totalUsd, exchangeRate = s.rate, itemsJson = json))
        }
        repo.clear(); onDone()
    }
}
 
