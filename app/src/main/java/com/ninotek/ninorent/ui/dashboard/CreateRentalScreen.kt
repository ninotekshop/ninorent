package com.ninotek.ninorent.ui.dashboard

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ninotek.ninorent.model.BankAccountInfo
import com.ninotek.ninorent.model.CartItem
import com.ninotek.ninorent.model.CreateRentalOrderState
import com.ninotek.ninorent.model.Customer
import com.ninotek.ninorent.model.Equipment
import com.ninotek.ninorent.model.LessorInfo
import com.ninotek.ninorent.model.PaperSize
import com.ninotek.ninorent.model.RentalOrder
import com.ninotek.ninorent.model.UserAccountStore
import com.ninotek.ninorent.model.UserRole
import com.ninotek.ninorent.model.defaultDevicesList
import com.ninotek.ninorent.model.defaultLessorInfo
import com.ninotek.ninorent.model.isStoreConfigured
import com.ninotek.ninorent.model.loadBankAccountInfoFromPrefs
import com.ninotek.ninorent.model.saveBankAccountInfoToPrefs
import com.ninotek.ninorent.model.saveLessorInfoToPrefs
import com.ninotek.ninorent.utils.SupabaseManager
import kotlinx.coroutines.launch
import com.ninotek.ninorent.ui.theme.NinoRentTheme
import com.ninotek.ninorent.ui.theme.PrimaryOrange
import com.ninotek.ninorent.utils.formatCurrencyAmount
import com.ninotek.ninorent.utils.formatCurrencyInput
import com.ninotek.ninorent.utils.getOrderEquipmentItems
import com.ninotek.ninorent.utils.parseCurrencyToLong
import com.ninotek.ninorent.utils.printContract
import com.ninotek.ninorent.utils.shareContractPdf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRentalScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    state: CreateRentalOrderState = remember { CreateRentalOrderState(defaultDevicesList) },
    lessorInfo: LessorInfo = defaultLessorInfo,
    bankAccountInfo: BankAccountInfo = loadBankAccountInfoFromPrefs(LocalContext.current),
    devicesList: List<Equipment> = defaultDevicesList,
    customersList: List<Customer> = emptyList(),
    onClearDemoData: () -> Unit = {},
    onCreateOrder: (RentalOrder) -> Unit = {},
    onPrintContract: (RentalOrder) -> Unit = {}
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val activeUser = remember { UserAccountStore.getCurrentSession(context) }
    val isOwner = activeUser == null || activeUser.role == UserRole.OWNER

    var isConfigured by remember { mutableStateOf(isStoreConfigured(context)) }
    var showSetupDialog by remember { mutableStateOf(!isConfigured) }
    var showClearDemoPrompt by remember { mutableStateOf(false) }

    if (!isConfigured && showSetupDialog) {
        if (isOwner) {
            var setupLessorName by remember { mutableStateOf(lessorInfo.name) }
            var setupLessorRep by remember { mutableStateOf(lessorInfo.representative) }
            var setupLessorAddr by remember { mutableStateOf(lessorInfo.address) }
            var setupLessorPhone by remember { mutableStateOf(lessorInfo.phone) }

            var setupBankName by remember { mutableStateOf(bankAccountInfo.bankName.ifBlank { "MB Bank" }) }
            var setupAccNum by remember { mutableStateOf(bankAccountInfo.accountNumber) }
            var setupAccHolder by remember { mutableStateOf(bankAccountInfo.accountHolderName) }

            AlertDialog(
                onDismissRequest = { onBack() },
                icon = { Icon(Icons.Rounded.Settings, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(36.dp)) },
                title = { Text("Cấu hình Cửa hàng (Bắt buộc)", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Đây là lần đầu tạo đơn. Vui lòng cấu hình Thông tin Bên A (Đơn vị cho thuê) và Tài khoản nhận tiền VietQR trước khi tiếp tục.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Text("1. Thông tin Bên A (Đơn vị cho thuê)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryOrange)

                        OutlinedTextField(
                            value = setupLessorName,
                            onValueChange = { setupLessorName = it },
                            label = { Text("Tên cửa hàng / Công ty cho thuê *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = setupLessorRep,
                            onValueChange = { setupLessorRep = it },
                            label = { Text("Người đại diện (Ông/Bà) *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = setupLessorAddr,
                            onValueChange = { setupLessorAddr = it },
                            label = { Text("Địa chỉ cửa hàng *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = setupLessorPhone,
                            onValueChange = { setupLessorPhone = it },
                            label = { Text("Số điện thoại hotline Bên A *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("2. Tài khoản Ngân hàng (VietQR)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryOrange)

                        OutlinedTextField(
                            value = setupBankName,
                            onValueChange = { setupBankName = it },
                            label = { Text("Tên Ngân hàng (vd: MB Bank, Vietcombank) *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = setupAccNum,
                            onValueChange = { setupAccNum = it },
                            label = { Text("Số tài khoản *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = setupAccHolder,
                            onValueChange = { setupAccHolder = it },
                            label = { Text("Tên chủ tài khoản (Viết hoa không dấu) *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (setupLessorName.isNotBlank() && setupLessorPhone.isNotBlank() && setupBankName.isNotBlank() && setupAccNum.isNotBlank()) {
                                val newLessor = LessorInfo(
                                    name = setupLessorName.trim(),
                                    representative = setupLessorRep.trim(),
                                    address = setupLessorAddr.trim(),
                                    phone = setupLessorPhone.trim()
                                )
                                val newBank = BankAccountInfo(
                                    bankName = setupBankName.trim(),
                                    accountNumber = setupAccNum.trim(),
                                    accountHolderName = setupAccHolder.trim()
                                )

                                saveLessorInfoToPrefs(context, newLessor)
                                saveBankAccountInfoToPrefs(context, newBank)

                                scope.launch {
                                    SupabaseManager.upsertStoreSettings(newLessor, newBank)
                                }

                                val prefs = context.getSharedPreferences("ninorent_prefs", Context.MODE_PRIVATE)
                                val hasCleared = prefs.getBoolean("hasClearedDemoData", false)
                                isConfigured = true
                                showSetupDialog = false
                                if (!hasCleared) {
                                    showClearDemoPrompt = true
                                } else {
                                    Toast.makeText(context, "Đã lưu cấu hình cửa hàng thành công!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Vui lòng điền đầy đủ các thông tin bắt buộc (*)", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        Text("Lưu & Tiếp tục Tạo đơn", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onBack() }) {
                        Text("Hủy (Quay lại)")
                    }
                }
            )
        } else {
            // Staff Warning Dialog
            AlertDialog(
                onDismissRequest = { onBack() },
                icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(36.dp)) },
                title = { Text("Cửa hàng chưa được cấu hình", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Cửa hàng chưa được cấu hình Thông tin Bên A (Đơn vị cho thuê) và Tài khoản ngân hàng VietQR.\n\nVui lòng báo với Chủ shop (Admin) thực hiện cấu hình trước khi tạo đơn.",
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { onBack() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        Text("Đóng / Quay lại")
                    }
                }
            )
        }
    }

    if (showClearDemoPrompt) {
        AlertDialog(
            onDismissRequest = { showClearDemoPrompt = false },
            icon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = Color.Red, modifier = Modifier.size(36.dp)) },
            title = { Text("Xóa dữ liệu Demo (Dữ liệu mẫu)", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Cấu hình cửa hàng đã hoàn tất!\n\nBạn có muốn dọn dẹp toàn bộ dữ liệu mẫu (thiết bị mẫu, đơn thuê mẫu, khách hàng mẫu) để bắt đầu nhập dữ liệu chính thức cho cửa hàng ngay bây giờ không?",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val prefs = context.getSharedPreferences("ninorent_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("hasClearedDemoData", true).apply()
                        onClearDemoData()
                        showClearDemoPrompt = false
                        Toast.makeText(context, "Đã dọn dẹp dữ liệu mẫu, cửa hàng sẵn sàng sử dụng chính thức!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa dữ liệu Demo ngay", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDemoPrompt = false }) {
                    Text("Để sau (Giữ dữ liệu mẫu)")
                }
            }
        )
    }

    BackHandler {
        when {
            state.showQrScannerDialog -> state.showQrScannerDialog = false
            state.currentStep > 1 -> state.currentStep -= 1
            else -> onBack()
        }
    }

    // Customer auto-fill on phone or CCCD match
    var lastMatchedCustId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.lesseePhone, state.lesseeIdNumber) {
        val cleanP = state.lesseePhone.replace(" ", "").trim()
        val cleanId = state.lesseeIdNumber.replace(" ", "").trim()
        if (cleanP.isNotBlank() || cleanId.isNotBlank()) {
            val matched = customersList.find { cust ->
                (cust.phone.isNotBlank() && cust.phone.replace(" ", "").trim() == cleanP) ||
                (cust.idNumber.isNotBlank() && cust.idNumber.replace(" ", "").trim() == cleanId)
            }
            if (matched != null && matched.id != lastMatchedCustId) {
                lastMatchedCustId = matched.id
                state.lesseeName = matched.name
                state.lesseePhone = matched.phone
                state.lesseeAddress = matched.address
                state.lesseeIdNumber = matched.idNumber
                state.lesseeIdIssueDate = matched.idIssueDate
                if (!matched.idCardFrontPhotoUri.isNullOrEmpty()) {
                    state.idCardFrontPhotoUri = matched.idCardFrontPhotoUri
                }
                if (!matched.idCardBackPhotoUri.isNullOrEmpty()) {
                    state.idCardBackPhotoUri = matched.idCardBackPhotoUri
                }
                Toast.makeText(context, "Đã tự động điền thông tin Khách hàng cũ: ${matched.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Calculation helper values
    val dailyTotal = state.cartItems.sumOf { item ->
        item.pricePerDay.filter { c -> c.isDigit() }.toLongOrNull() ?: 0L
    }
    val daysCount = state.durationDays.filter { it.isDigit() }.toIntOrNull() ?: 1
    val grossTotal = dailyTotal * daysCount

    val rawDiscount = if (!state.discountTypeIsPercent) {
        state.discountValueText.filter { it.isDigit() }.toLongOrNull() ?: 0L
    } else {
        val percent = state.discountValueText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
        (grossTotal * (percent / 100.0)).toLong()
    }

    val netTotal = (grossTotal - rawDiscount).coerceAtLeast(0L)

    val grossTotalFormatted = formatCurrencyAmount(grossTotal.toString())
    val discountFormatted = if (state.discountTypeIsPercent) {
        val pStr = state.discountValueText.ifEmpty { "0" }
        "$pStr% (${formatCurrencyAmount(rawDiscount.toString())})"
    } else {
        formatCurrencyAmount(rawDiscount.toString())
    }
    val netTotalFormatted = formatCurrencyAmount(netTotal.toString())
    val dailyTotalFormatted = formatCurrencyAmount(dailyTotal.toString())

    // Scroll Position Reset when entering Step 3 & Step 4 & default advance payment amount
    LaunchedEffect(state.currentStep) {
        if (state.currentStep == 3 || state.currentStep == 4) {
            scrollState.scrollTo(0)
        }
        if (state.currentStep == 3) {
            state.advancePaymentAmount = netTotalFormatted
        }
    }

    // Camera & Image Launchers
    val frontPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) state.idCardFrontPhotoUri = uri.toString() }

    val frontCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> if (bitmap != null) state.idCardFrontPhotoBitmap = bitmap }

    val backPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) state.idCardBackPhotoUri = uri.toString() }

    val backCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> if (bitmap != null) state.idCardBackPhotoBitmap = bitmap }

    val equipmentNameCombined = state.cartItems.joinToString(" + ") { it.equipmentName }.ifEmpty { "Sony Alpha A7C" }
    val serialNumberCombined = state.cartItems.joinToString(", ") { it.serialNumber }.ifEmpty { "SN-2026-NINO88" }

    val currentCreatedOrder = remember(
        state.generatedOrderId, equipmentNameCombined, serialNumberCombined, state.startDate, state.startTime, state.endDate, state.endTime, state.durationDays,
        netTotalFormatted, state.lesseeName, state.lesseePhone, state.lesseeAddress, state.lesseeIdNumber, state.lesseeIdIssueDate,
        state.advancePaymentAmount, state.collateralCccd, state.collateralGplx, state.hasCollateralAsset, state.collateralAssetDescription,
        state.hasCollateralCash, state.collateralCashAmount, state.contractLocation, state.contractDate, lessorInfo, bankAccountInfo, discountFormatted, grossTotalFormatted, state.cartItems.toList()
    ) {
        RentalOrder(
            id = state.generatedOrderId,
            equipmentName = equipmentNameCombined,
            dateRange = "${state.startTime} ${state.startDate} - ${state.endTime} ${state.endDate} (${state.durationDays})",
            price = grossTotalFormatted,
            status = "Đang thuê",
            customerName = state.lesseeName.ifBlank { "Khách Thuê" },
            serialNumber = serialNumberCombined,
            discountAmount = discountFormatted,
            netTotal = netTotalFormatted,
            equipmentItems = state.cartItems.toList(),
            bankName = bankAccountInfo.bankName,
            bankAccountNumber = bankAccountInfo.accountNumber,
            bankAccountHolder = bankAccountInfo.accountHolderName,
            lessorName = lessorInfo.name,
            lessorAddress = lessorInfo.address,
            lessorRepresentative = lessorInfo.representative,
            lessorPhone = lessorInfo.phone,
            lesseeName = state.lesseeName,
            lesseeAddress = state.lesseeAddress,
            lesseePhone = state.lesseePhone,
            lesseeIdNumber = state.lesseeIdNumber,
            lesseeIdIssueDate = state.lesseeIdIssueDate,
            advancePaymentAmount = formatCurrencyAmount(state.advancePaymentAmount),
            collateralCccd = state.collateralCccd,
            collateralGplx = state.collateralGplx,
            collateralAssetDescription = if (state.hasCollateralAsset) state.collateralAssetDescription else "",
            collateralCashAmount = if (state.hasCollateralCash) formatCurrencyAmount(state.collateralCashAmount) else "0",
            idCardFrontPhotoUri = state.idCardFrontPhotoUri,
            idCardBackPhotoUri = state.idCardBackPhotoUri,
            contractLocation = state.contractLocation,
            contractDate = state.contractDate
        )
    }

    var isOrderSaved by remember(state.generatedOrderId) { mutableStateOf(false) }

    val confirmPaymentAndSaveOrder = {
        state.paymentConfirmed = true
        if (!isOrderSaved) {
            onCreateOrder(currentCreatedOrder)
            isOrderSaved = true
            Toast.makeText(context, "Đã xác nhận thanh toán & lưu đơn thuê vào CSDL!", Toast.LENGTH_SHORT).show()
        }
        state.currentStep = 4
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tạo Đơn Thuê Thiết Bị", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Bước ${state.currentStep} / 5 - ${getStepTitle(state.currentStep)}", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.currentStep > 1) state.currentStep -= 1 else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Stepper Header: [1] Khách hàng -> [2] Thiết bị & Seri -> [3] Thanh toán & VietQR -> [4] Hợp đồng -> [5] Hoàn tất
            StepperHeader(
                currentStep = state.currentStep,
                onStepClick = { step -> state.currentStep = step }
            )

            if (state.scanSuccessMessage != null && state.currentStep == 1) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                            Text(
                                text = state.scanSuccessMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                        IconButton(onClick = { state.scanSuccessMessage = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (state.currentStep) {
                    1 -> {
                        // ================= STEP 1: THÔNG TIN KHÁCH THUÊ =================
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PrimaryOrange.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = PrimaryOrange)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Quét nhanh CCCD từ QR Code", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Tự động điền Họ tên, Số CCCD, Địa chỉ", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { state.showQrScannerDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Quét", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false)
                                }
                            }
                        }

                        Text("1. Thông tin Khách thuê", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        val cleanPhone = state.lesseePhone.replace(" ", "").trim()
                        val matchedCustomer = remember(cleanPhone, customersList) {
                            if (cleanPhone.length >= 6) {
                                customersList.find { cust ->
                                    cust.phone.replace(" ", "").trim() == cleanPhone
                                }
                            } else null
                        }

                        OutlinedTextField(
                            value = state.lesseePhone,
                            onValueChange = { state.lesseePhone = it },
                            label = { Text("Số điện thoại *") },
                            leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                            placeholder = { Text("vd: 0901234567") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )

                        if (cleanPhone.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (matchedCustomer != null) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                ) {
                                    Text(
                                        text = if (matchedCustomer != null) "✓ Khách hàng cũ: ${matchedCustomer.name} (Đã có trong hệ thống)" else "➕ Khách hàng mới",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (matchedCustomer != null) Color(0xFF2E7D32) else PrimaryOrange
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = state.lesseeName,
                            onValueChange = { state.lesseeName = it },
                            label = { Text("Họ và tên khách hàng *") },
                            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                            placeholder = { Text("Nhập họ và tên khách thuê") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = state.lesseeAddress,
                            onValueChange = { state.lesseeAddress = it },
                            label = { Text("Địa chỉ thường trú / Chỗ ở hiện tại *") },
                            leadingIcon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                            placeholder = { Text("vd: 123 Lê Lợi, TP. Quy Nhơn, Bình Định") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = state.lesseeIdNumber,
                            onValueChange = { state.lesseeIdNumber = it },
                            label = { Text("Số CCCD / GPLX *") },
                            leadingIcon = { Icon(Icons.Rounded.Badge, contentDescription = null) },
                            placeholder = { Text("vd: 052095001234") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        OutlinedTextField(
                            value = state.lesseeIdIssueDate,
                            onValueChange = { state.lesseeIdIssueDate = it },
                            label = { Text("Ngày cấp") },
                            leadingIcon = { Icon(Icons.Rounded.CalendarToday, contentDescription = null) },
                            placeholder = { Text("vd: 15/05/2021") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Text("2. Ảnh chụp giấy tờ tùy thân", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IdCardPhotoPickerBox(
                                title = "Mặt trước CCCD",
                                photoUri = state.idCardFrontPhotoUri,
                                photoBitmap = state.idCardFrontPhotoBitmap,
                                onPickPhotoFromGallery = { frontPhotoLauncher.launch("image/*") },
                                onTakePhotoFromCamera = { frontCameraLauncher.launch(null) },
                                onRetakePhoto = { frontCameraLauncher.launch(null) },
                                onClearPhoto = {
                                    state.idCardFrontPhotoUri = null
                                    state.idCardFrontPhotoBitmap = null
                                },
                                modifier = Modifier.weight(1f)
                            )

                            IdCardPhotoPickerBox(
                                title = "Mặt sau CCCD",
                                photoUri = state.idCardBackPhotoUri,
                                photoBitmap = state.idCardBackPhotoBitmap,
                                onPickPhotoFromGallery = { backPhotoLauncher.launch("image/*") },
                                onTakePhotoFromCamera = { backCameraLauncher.launch(null) },
                                onRetakePhoto = { backCameraLauncher.launch(null) },
                                onClearPhoto = {
                                    state.idCardBackPhotoUri = null
                                    state.idCardBackPhotoBitmap = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { state.currentStep = 2 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                        ) {
                            Text("Tiếp tục: Chọn thiết bị ➔", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    2 -> {
                        // ================= STEP 2: CHỌN THIẾT BỊ & SERI & GIÁ =================
                        var isDropdownExpanded by remember { mutableStateOf(false) }
                        var equipmentSearchQuery by remember { mutableStateOf("") }

                        Text("1. Chọn thiết bị từ danh mục", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        val catalogOptions = devicesList

                        val filteredCatalogOptions = catalogOptions.filter { device ->
                            device.name.contains(equipmentSearchQuery, ignoreCase = true) ||
                            device.categorySubtitle.contains(equipmentSearchQuery, ignoreCase = true) ||
                            device.serialNumber.contains(equipmentSearchQuery, ignoreCase = true)
                        }

                        ExposedDropdownMenuBox(
                            expanded = isDropdownExpanded,
                            onExpandedChange = { isDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = equipmentSearchQuery,
                                onValueChange = {
                                    equipmentSearchQuery = it
                                    isDropdownExpanded = true
                                },
                                placeholder = { Text("Chọn hoặc tìm kiếm thiết bị...") },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true),
                                shape = RoundedCornerShape(12.dp)
                            )

                            if (filteredCatalogOptions.isNotEmpty()) {
                                ExposedDropdownMenu(
                                    expanded = isDropdownExpanded,
                                    onDismissRequest = { isDropdownExpanded = false },
                                    modifier = Modifier.heightIn(max = 280.dp)
                                ) {
                                    filteredCatalogOptions.forEach { device ->
                                        val rawPrice = device.pricePerDay.filter { c -> c.isDigit() }.ifEmpty { "2000000" }
                                        val isInCart = state.cartItems.any { it.equipmentName == device.name }

                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(device.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        if (device.categorySubtitle.isNotBlank()) {
                                                            Text(device.categorySubtitle, fontSize = 11.sp, color = Color.Gray)
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(formatCurrencyAmount(rawPrice), fontWeight = FontWeight.Bold, color = PrimaryOrange, fontSize = 12.sp)
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = if (isInCart) Icons.Rounded.CheckCircle else Icons.Rounded.AddCircleOutline,
                                                    contentDescription = null,
                                                    tint = if (isInCart) PrimaryOrange else Color.Gray
                                                )
                                            },
                                            onClick = {
                                                state.cartItems.add(
                                                    CartItem(
                                                        equipmentName = device.name,
                                                        pricePerDay = rawPrice,
                                                        serialNumber = device.serialNumber.ifEmpty { "SN-${(1000..9999).random()}" }
                                                    )
                                                )
                                                Toast.makeText(context, "Đã thêm ${device.name} vào đơn thuê", Toast.LENGTH_SHORT).show()
                                                equipmentSearchQuery = ""
                                                isDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Shopping Cart Section: List of Selected Equipment Items
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("2. Giỏ hàng thiết bị đã chọn (${state.cartItems.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (state.cartItems.isNotEmpty()) {
                                TextButton(onClick = { state.cartItems.clear() }) {
                                    Text("Xóa tất cả", color = Color.Red, fontSize = 12.sp)
                                }
                            }
                        }

                        if (state.cartItems.isEmpty()) {
                            Surface(
                                color = Color(0xFFFFF3E0),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Chưa có thiết bị nào trong đơn thuê. Vui lòng bấm 'Thêm' từ danh mục trên.",
                                    modifier = Modifier.padding(14.dp),
                                    fontSize = 12.sp,
                                    color = Color(0xFFE65100),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                state.cartItems.forEachIndexed { index, cartItem ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.4f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = PrimaryOrange,
                                                        modifier = Modifier.size(20.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                        }
                                                    }
                                                    Text(cartItem.equipmentName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = formatCurrencyAmount(cartItem.pricePerDay),
                                                        fontWeight = FontWeight.Bold,
                                                        color = PrimaryOrange,
                                                        fontSize = 13.sp
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    IconButton(
                                                        onClick = { state.cartItems.remove(cartItem) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Rounded.Delete, contentDescription = "Xóa khỏi giỏ", tint = Color.Red, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }

                                            OutlinedTextField(
                                                value = cartItem.serialNumber,
                                                onValueChange = { newSerial ->
                                                    val idx = state.cartItems.indexOf(cartItem)
                                                    if (idx >= 0) {
                                                        state.cartItems[idx] = cartItem.copy(serialNumber = newSerial)
                                                    }
                                                },
                                                label = { Text("Số Serial (Serial Number) *") },
                                                leadingIcon = { Icon(Icons.Rounded.QrCode, contentDescription = null) },
                                                placeholder = { Text("vd: SN-2026-NINO88") },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                singleLine = true
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text("3. Thời gian & Giảm giá (Nguyên tắc 24h)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Surface(
                            color = PrimaryOrange.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Tính theo chu kỳ 24h: Giờ nhận máy trùng giờ trả máy sau N ngày (Ví dụ: ${state.startTime} ${state.startDate} ➔ ${state.endTime} ${state.endDate} là ${state.durationDays}).",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = state.startDate,
                                onValueChange = { state.startDate = it },
                                label = { Text("Ngày bắt đầu") },
                                leadingIcon = { Icon(Icons.Rounded.CalendarToday, contentDescription = null) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = state.startTime,
                                onValueChange = {
                                    state.startTime = it
                                    state.endTime = it
                                },
                                label = { Text("Giờ nhận máy") },
                                leadingIcon = { Icon(Icons.Rounded.Schedule, contentDescription = null) },
                                placeholder = { Text("vd: 19:00") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = state.endDate,
                                onValueChange = { state.endDate = it },
                                label = { Text("Ngày kết thúc") },
                                leadingIcon = { Icon(Icons.Rounded.CalendarToday, contentDescription = null) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = state.endTime,
                                onValueChange = { state.endTime = it },
                                label = { Text("Giờ trả máy (24h)") },
                                leadingIcon = { Icon(Icons.Rounded.Schedule, contentDescription = null) },
                                placeholder = { Text("vd: 19:00") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = state.durationDays,
                            onValueChange = { state.durationDays = it },
                            label = { Text("Số ngày thuê (24h/ngày)") },
                            leadingIcon = { Icon(Icons.Rounded.Timelapse, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Discount Options (VNĐ vs %)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Loại giảm giá:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = !state.discountTypeIsPercent,
                                    onClick = { state.discountTypeIsPercent = false },
                                    label = { Text("Số tiền (VNĐ)", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = state.discountTypeIsPercent,
                                    onClick = { state.discountTypeIsPercent = true },
                                    label = { Text("Phần trăm (%)", fontSize = 11.sp) }
                                )
                            }
                        }

                        // Discount Amount / Percent Field
                        OutlinedTextField(
                            value = state.discountValueText,
                            onValueChange = { state.discountValueText = if (!state.discountTypeIsPercent) formatCurrencyInput(it) else it },
                            label = {
                                Text(
                                    if (!state.discountTypeIsPercent) "Giảm giá (Số tiền - VNĐ)"
                                    else "Giảm giá (Phần trăm - %)"
                                )
                            },
                            leadingIcon = { Icon(Icons.Rounded.Discount, contentDescription = null) },
                            placeholder = {
                                Text(if (!state.discountTypeIsPercent) "vd: 500.000" else "vd: 10")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        // Calculation Summary Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Tổng đơn giá (${state.cartItems.size} thiết bị):", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                    Text(dailyTotalFormatted, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Tổng tiền ban đầu ($daysCount ngày):", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                    Text(grossTotalFormatted, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Giảm giá:", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                    Text("- $discountFormatted", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Thành tiền thực tế:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = netTotalFormatted,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = PrimaryOrange,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { state.currentStep = 1 },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("⇦ Quay lại", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { state.currentStep = 3 },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                            ) {
                                Text("Tiếp tục thanh toán ➔", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    3 -> {
                        // ================= STEP 3: THANH TOÁN & THẾ CHẤP + VIETQR =================
                        Text("1. Giấy tờ & Tài sản thế chấp (giữ lại)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { state.collateralCccd = !state.collateralCccd }
                                ) {
                                    Checkbox(checked = state.collateralCccd, onCheckedChange = { state.collateralCccd = it }, colors = CheckboxDefaults.colors(checkedColor = PrimaryOrange))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Giữ CCCD gốc của khách hàng", fontWeight = FontWeight.Medium)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { state.collateralGplx = !state.collateralGplx }
                                ) {
                                    Checkbox(checked = state.collateralGplx, onCheckedChange = { state.collateralGplx = it }, colors = CheckboxDefaults.colors(checkedColor = PrimaryOrange))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Giữ Giấy phép lái xe (GPLX) gốc", fontWeight = FontWeight.Medium)
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { state.hasCollateralAsset = !state.hasCollateralAsset }
                                    ) {
                                        Checkbox(checked = state.hasCollateralAsset, onCheckedChange = { state.hasCollateralAsset = it }, colors = CheckboxDefaults.colors(checkedColor = PrimaryOrange))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Giữ tài sản khác", fontWeight = FontWeight.Medium)
                                    }
                                    AnimatedVisibility(visible = state.hasCollateralAsset) {
                                        OutlinedTextField(
                                            value = state.collateralAssetDescription,
                                            onValueChange = { state.collateralAssetDescription = it },
                                            label = { Text("Tên / mô tả tài sản thế chấp") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 40.dp, top = 4.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { state.hasCollateralCash = !state.hasCollateralCash }
                                    ) {
                                        Checkbox(checked = state.hasCollateralCash, onCheckedChange = { state.hasCollateralCash = it }, colors = CheckboxDefaults.colors(checkedColor = PrimaryOrange))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Tiền đặt cọc thế chấp", fontWeight = FontWeight.Medium)
                                    }
                                    AnimatedVisibility(visible = state.hasCollateralCash) {
                                        OutlinedTextField(
                                            value = state.collateralCashAmount,
                                            onValueChange = { state.collateralCashAmount = it },
                                            label = { Text("Số tiền cọc thế chấp (VNĐ)") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 40.dp, top = 4.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }

                        Text("2. Phương thức & Tiền đặt cọc", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        // Payment Method choice: Tiền mặt vs Chuyển khoản VietQR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = state.paymentMethod == "Tiền mặt",
                                onClick = { state.paymentMethod = "Tiền mặt" },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Tiền mặt", fontWeight = FontWeight.Bold)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = state.paymentMethod == "Chuyển khoản VietQR",
                                onClick = { state.paymentMethod = "Chuyển khoản VietQR" },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Rounded.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Chuyển khoản VietQR", fontWeight = FontWeight.Bold)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = state.advancePaymentAmount,
                            onValueChange = { state.advancePaymentAmount = formatCurrencyInput(it) },
                            label = { Text("Số tiền thanh toán trước (VNĐ)") },
                            leadingIcon = { Icon(Icons.Rounded.Payments, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // VietQR Bank Transfer Card
                        if (state.paymentMethod == "Chuyển khoản VietQR") {
                            val cleanBankName = bankAccountInfo.bankName.replace(" ", "")
                            val cleanAccNum = bankAccountInfo.accountNumber.filter { it.isDigit() }
                            val advanceLong = parseCurrencyToLong(state.advancePaymentAmount)
                            val payAmount = if (advanceLong > 0) advanceLong.toString() else netTotal.toString()
                            val vietQrUrl = "https://img.vietqr.io/image/$cleanBankName-$cleanAccNum-compact2.png?amount=$payAmount&addInfo=${state.generatedOrderId.replace("#", "")}"

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.5.dp, PrimaryOrange.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = PrimaryOrange)
                                            Text("Chuyển khoản VietQR", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        }
                                    }

                                    HorizontalDivider()

                                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("• Ngân hàng: ${bankAccountInfo.bankName}", fontWeight = FontWeight.Bold)
                                        Text("• Số tài khoản: ${bankAccountInfo.accountNumber}", fontWeight = FontWeight.Bold, color = PrimaryOrange)
                                        Text("• Chủ tài khoản: ${bankAccountInfo.accountHolderName}", fontWeight = FontWeight.Bold)
                                        Text("• Số tiền thanh toán: ${formatCurrencyAmount(payAmount)}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                        Text("• Nội dung chuyển khoản: ${state.generatedOrderId.replace("#", "")}", fontWeight = FontWeight.Bold)
                                    }

                                    // VietQR Image Component
                                    Box(
                                        modifier = Modifier
                                            .size(200.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White)
                                            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = vietQrUrl,
                                            contentDescription = "VietQR Code",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Text(
                                        text = "Quét mã QR bằng ứng dụng Mobile Banking bất kỳ để chuyển khoản",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Button(
                                        onClick = { confirmPaymentAndSaveOrder() },
                                        modifier = Modifier.fillMaxWidth().height(46.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Xác nhận chuyển khoản thành công", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        } else {
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Payments, contentDescription = null, tint = Color(0xFF2E7D32))
                                    Text("Khách hàng đã chọn thanh toán bằng Tiền mặt trực tiếp tại cửa hàng.", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), fontSize = 13.sp)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { state.currentStep = 2 },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("⇦ Quay lại", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { confirmPaymentAndSaveOrder() },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                            ) {
                                Text("Tiếp tục: Xem Hợp đồng ➔", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    4 -> {
                        // ================= STEP 4: XEM TRƯỚC HỢP ĐỒNG & IN HỢP ĐỒNG =================
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Chọn khổ giấy:", fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = state.selectedPaperSize == PaperSize.A4,
                                        onClick = { state.selectedPaperSize = PaperSize.A4 },
                                        label = { Text("Khổ A4") }
                                    )
                                    FilterChip(
                                        selected = state.selectedPaperSize == PaperSize.A5,
                                        onClick = { state.selectedPaperSize = PaperSize.A5 },
                                        label = { Text("Khổ A5") }
                                    )
                                }
                            }
                        }

                        // Full Contract Document Card Component
                        PaperDocumentCard(
                            order = currentCreatedOrder,
                            paperSize = state.selectedPaperSize,
                            lessorInfo = lessorInfo
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { shareContractPdf(context, currentCreatedOrder, state.selectedPaperSize) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chia sẻ PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    onPrintContract(currentCreatedOrder)
                                    printContract(context, currentCreatedOrder, state.selectedPaperSize)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                            ) {
                                Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("In hợp đồng", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { state.currentStep = 3 },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("⇦ Quay lại", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { state.currentStep = 5 },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                            ) {
                                Text("Tiếp tục: Hoàn tất ➔", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    5 -> {
                        // ================= STEP 5: HOÀN TẤT & LƯU ĐƠN =================
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8F5E9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(48.dp))
                                }

                                Text("Tạo Đơn Thuê & Hợp Đồng Thành Công!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PrimaryOrange)

                                Surface(shape = RoundedCornerShape(8.dp), color = PrimaryOrange.copy(alpha = 0.15f)) {
                                    Text("Mã Đơn: ${currentCreatedOrder.id}", modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), fontWeight = FontWeight.ExtraBold, color = PrimaryOrange, fontSize = 16.sp)
                                }

                                HorizontalDivider()

                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Khách hàng:", color = Color.Gray)
                                        Text(currentCreatedOrder.customerName, fontWeight = FontWeight.Bold)
                                    }

                                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Thiết bị thuê:", color = Color.Gray)
                                        val itemsList = getOrderEquipmentItems(currentCreatedOrder)
                                        itemsList.forEach { item ->
                                            Text(
                                                text = "• ${item.equipmentName}${if (item.serialNumber.isNotBlank()) " (Seri: ${item.serialNumber})" else ""}",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Thời gian thuê:", color = Color.Gray)
                                        Text(
                                            text = currentCreatedOrder.dateRange,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Thành tiền thực tế:", color = Color.Gray)
                                        Text(currentCreatedOrder.netTotal, fontWeight = FontWeight.ExtraBold, color = PrimaryOrange)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Trạng thái:", color = Color.Gray)
                                        Text(currentCreatedOrder.status, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        if (!isOrderSaved) {
                                            onCreateOrder(currentCreatedOrder)
                                            isOrderSaved = true
                                        }
                                        state.reset(devicesList)
                                        onSuccess()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Hoàn tất", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // CameraX + ML Kit CCCD QR Code Scanner Sheet
    if (state.showQrScannerDialog) {
        CccdCameraScannerSheet(
            onDismiss = { state.showQrScannerDialog = false },
            onCccdScanned = { cccdData ->
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                state.lesseeIdNumber = cccdData.cccdNumber
                state.lesseeName = cccdData.fullName
                state.lesseeAddress = cccdData.address
                state.lesseeIdIssueDate = cccdData.issueDate
                state.scanSuccessMessage = "Đã trích xuất thông tin CCCD: ${cccdData.fullName} (${cccdData.cccdNumber})"
                Toast.makeText(context, "Đã quét mã QR CCCD thành công!", Toast.LENGTH_SHORT).show()
                state.showQrScannerDialog = false
            }
        )
    }
}

@Composable
fun IdCardPhotoPickerBox(
    title: String,
    photoUri: String?,
    photoBitmap: Bitmap? = null,
    onPickPhotoFromGallery: () -> Unit,
    onTakePhotoFromCamera: () -> Unit,
    onRetakePhoto: () -> Unit,
    onClearPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (photoUri != null || photoBitmap != null) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
        ),
        border = if (photoUri == null && photoBitmap == null) {
            BorderStroke(1.dp, Color.LightGray)
        } else {
            BorderStroke(1.5.dp, Color(0xFF2E7D32))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (photoUri != null || photoBitmap != null) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
            )

            if (photoUri != null || photoBitmap != null) {
                // Small thumbnail image preview (~80dp) with check badge
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    if (photoBitmap != null) {
                        Image(
                            bitmap = photoBitmap.asImageBitmap(),
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF2E7D32), RoundedCornerShape(12.dp))
                        )
                    } else if (!photoUri.isNull_or_blank()) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF2E7D32), RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFC8E6C9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Badge, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(36.dp))
                        }
                    }

                    // Green Check Badge
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier
                            .size(22.dp)
                            .offset(x = 4.dp, y = (-4).dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = onRetakePhoto, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryOrange)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Chụp lại", fontSize = 11.sp, color = PrimaryOrange, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onClearPhoto, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text("Xóa", fontSize = 11.sp, color = Color.Red)
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddAPhoto,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Chưa có ảnh",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = onTakePhotoFromCamera,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Chụp", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onPickPhotoFromGallery,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Chọn", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.isBlank()
}

fun getStepTitle(step: Int): String {
    return when (step) {
        1 -> "Thông tin Khách thuê"
        2 -> "Chọn Thiết bị & Seri"
        3 -> "Thanh toán & VietQR"
        4 -> "Hợp đồng & In ấn"
        5 -> "Hoàn tất đơn thuê"
        else -> ""
    }
}

@Composable
fun StepperHeader(
    currentStep: Int,
    onStepClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        "1" to "Khách thuê",
        "2" to "Thiết bị & Seri",
        "3" to "Thanh toán & VietQR",
        "4" to "Hợp đồng",
        "5" to "Hoàn tất"
    )

    ScrollableTabRow(
        selectedTabIndex = currentStep - 1,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = PrimaryOrange,
        edgePadding = 12.dp,
        modifier = modifier
    ) {
        steps.forEachIndexed { index, (num, label) ->
            val stepNumber = index + 1
            val isSelected = currentStep == stepNumber
            val isCompleted = currentStep > stepNumber

            Tab(
                selected = isSelected,
                onClick = {
                    if (stepNumber <= currentStep || isCompleted) {
                        onStepClick(stepNumber)
                    }
                },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when {
                                isSelected -> PrimaryOrange
                                isCompleted -> Color(0xFF2E7D32)
                                else -> Color.LightGray
                            },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isCompleted) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Text(
                                        text = num,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isSelected -> PrimaryOrange
                                isCompleted -> Color(0xFF2E7D32)
                                else -> Color.Gray
                            }
                        )
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateRentalScreenPreview() {
    NinoRentTheme {
        CreateRentalScreen(onBack = {}, onSuccess = {})
    }
}
