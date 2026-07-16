package com.saad.tvcast.feature.premium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.saad.tvcast.R
import com.saad.tvcast.core.billing.BillingManager
import com.saad.tvcast.core.designsystem.component.DeviceRow
import com.saad.tvcast.core.designsystem.component.LoadingPanel
import com.saad.tvcast.core.designsystem.component.StatusCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {
    val entitlement = billingManager.entitlement
    private val _products = MutableStateFlow<List<ProductDetails>?>(null)
    val products: StateFlow<List<ProductDetails>?> = _products

    fun load() {
        viewModelScope.launch { _products.value = billingManager.queryProducts() }
    }

    fun restore() {
        viewModelScope.launch { billingManager.restorePurchases() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    viewModel: PremiumViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val entitlement by viewModel.entitlement.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.premium)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatusCard(stringResource(R.string.premium_title), stringResource(R.string.premium_body), Icons.Outlined.WorkspacePremium)
            if (products == null) {
                LoadingPanel()
            } else if (products.orEmpty().isEmpty()) {
                StatusCard(stringResource(R.string.purchase_options), stringResource(R.string.billing_unavailable))
            } else {
                products.orEmpty().forEach { product ->
                    DeviceRow(product.name, product.description, onClick = { })
                }
            }
            entitlement.message?.let { StatusCard(stringResource(R.string.restore_purchase), it) }
            Button(onClick = viewModel::restore) { Text(stringResource(R.string.restore_purchase)) }
        }
    }
}
