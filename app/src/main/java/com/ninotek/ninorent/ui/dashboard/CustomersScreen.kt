package com.ninotek.ninorent.ui.dashboard

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ninotek.ninorent.model.Customer
import com.ninotek.ninorent.ui.theme.NinoRentTheme
import com.ninotek.ninorent.ui.theme.PrimaryOrange
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

val defaultCustomersList = listOf(
    Customer("1", "Nguyễn Văn Nam", "090 123 4567", "123 Lê Lợi, TP. Quy Nhơn, Gia Lai", "Cá nhân", "nam.nguyen@example.com", "012345678901"),
    Customer("2", "Lê Thị Mai", "098 765 4321", "45 Nguyễn Trãi, TP. Quy Nhơn, Bình Định", "Cá nhân", "mai.le@example.com", "052200001234"),
    Customer("3", "Công ty TNHH ABC", "024 3838 9999", "Tòa Keangnam, Nam Từ Liêm, Hà Nội", "Công ty", "contact@abc.vn", "0101234567"),
    Customer("4", "Hoàng Minh Tuấn", "090 987 6543", "12 Hai Bà Trưng, Hoàn Kiếm, Hà Nội", "Cá nhân", "tuan.hoang@example.com", "034098765432"),
    Customer("5", "Công ty Truyền thông Media Pro", "028 3939 8888", "Quận 1, TP. Hồ Chí Minh", "Công ty", "info@mediapro.vn", "0309876543"),
    Customer("6", "Trần Quốc Bảo", "091 222 3344", "78 Láng Hạ, Đống Đa, Hà Nội", "Cá nhân", "bao.tran@example.com", "001091222334")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    customerList: List<Customer> = defaultCustomersList,
    onAddCustomer: (Customer) -> Unit = {},
    onDeleteCustomer: (Customer) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Tất cả") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val individualCount = customerList.count { it.type == "Cá nhân" }
    val companyCount = customerList.count { it.type == "Công ty" }
    val totalCount = customerList.size

    val filterPills = listOf("Tất cả ($totalCount)", "Cá nhân ($individualCount)", "Công ty ($companyCount)")

    val filteredCustomers = customerList.filter { customer ->
        val matchesSearch = customer.name.contains(searchQuery, ignoreCase = true) ||
                customer.phone.contains(searchQuery, ignoreCase = true) ||
                customer.address.contains(searchQuery, ignoreCase = true) ||
                customer.idNumber.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "Cá nhân" -> customer.type == "Cá nhân"
            "Công ty" -> customer.type == "Công ty"
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Khách hàng", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Rounded.PersonAdd, contentDescription = "Thêm khách hàng", tint = PrimaryOrange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryOrange,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Thêm khách hàng")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm kiếm khách hàng (Tên, SĐT, CCCD)...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Xóa")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    unfocusedBorderColor = Color.LightGray,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterPills.forEach { pill ->
                    val pillName = pill.substringBefore(" (")
                    val isSelected = selectedFilter == pillName || (selectedFilter == "Tất cả" && pill.startsWith("Tất cả"))
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedFilter = if (pillName == "Tất cả") "Tất cả" else pillName
                        },
                        label = { Text(pill, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryOrange.copy(alpha = 0.15f),
                            selectedLabelColor = PrimaryOrange
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Customer List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp)
            ) {
                items(filteredCustomers) { customer ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedCustomer = customer
                                showDetailDialog = true
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryOrange.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (customer.type == "Công ty") Icons.Rounded.Business else Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = PrimaryOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = customer.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val isCompany = customer.type == "Công ty"
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCompany) Color(0xFFE3F2FD) else Color(0xFFFFF3E0)
                                    ) {
                                        Text(
                                            text = customer.type,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCompany) Color(0xFF1565C0) else Color(0xFFEF6C00)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.Phone, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = customer.phone,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PrimaryOrange,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = customer.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        maxLines = 1
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    customerToDelete = customer
                                    showDeleteConfirmDialog = true
                                }
                            ) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "Xóa khách hàng",
                                    tint = Color.Red.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Customer Dialog ("Thêm khách hàng")
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("Cá nhân") }
        var email by remember { mutableStateOf("") }
        var idNumber by remember { mutableStateOf("") }
        var showQrScannerDialog by remember { mutableStateOf(false) }

        var frontPhotoUri by remember { mutableStateOf<String?>(null) }
        var frontPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var backPhotoUri by remember { mutableStateOf<String?>(null) }
        var backPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }

        val context = LocalContext.current
        val hapticFeedback = LocalHapticFeedback.current

        val frontPhotoLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri -> 
            if (uri != null) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val file = File(context.filesDir, "cust_front_gal_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { output ->
                        inputStream?.copyTo(output)
                    }
                    inputStream?.close()
                    frontPhotoUri = file.toURI().toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                    frontPhotoUri = uri.toString()
                }
            } 
        }

        val frontCameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { bitmap -> 
            if (bitmap != null) {
                frontPhotoBitmap = bitmap
                try {
                    val file = File(context.filesDir, "cust_front_cam_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    frontPhotoUri = file.toURI().toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } 
        }

        val backPhotoLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri -> 
            if (uri != null) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val file = java.io.File(context.filesDir, "cust_back_gal_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { output ->
                        inputStream?.copyTo(output)
                    }
                    inputStream?.close()
                    backPhotoUri = file.toURI().toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                    backPhotoUri = uri.toString()
                }
            } 
        }

        val backCameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { bitmap -> 
            if (bitmap != null) {
                backPhotoBitmap = bitmap
                try {
                    val file = java.io.File(context.filesDir, "cust_back_cam_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    backPhotoUri = file.toURI().toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } 
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Thêm khách hàng mới", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Loại khách hàng:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = type == "Cá nhân",
                            onClick = { type = "Cá nhân" },
                            label = { Text("Cá nhân") }
                        )
                        FilterChip(
                            selected = type == "Công ty",
                            onClick = { type = "Công ty" },
                            label = { Text("Công ty") }
                        )
                    }

                    if (type == "Cá nhân") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = PrimaryOrange.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
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
                                        Text("Quét nhanh CCCD từ QR Code", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Tự động điền Họ tên, Số CCCD, Địa chỉ", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Button(
                                    onClick = { showQrScannerDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Quét", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tên khách hàng / Công ty *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Số điện thoại *") },
                        singleLine = true,
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = idNumber,
                        onValueChange = { idNumber = it },
                        label = { Text(if (type == "Công ty") "Mã số thuế" else "Số CCCD / GPLX") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Địa chỉ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (type == "Cá nhân") {
                        Text("Ảnh chụp giấy tờ tùy thân:", fontWeight = FontWeight.Medium, fontSize = 13.sp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IdCardPhotoPickerBox(
                                title = "Mặt trước",
                                photoUri = frontPhotoUri,
                                photoBitmap = frontPhotoBitmap,
                                onPickPhotoFromGallery = { frontPhotoLauncher.launch("image/*") },
                                onTakePhotoFromCamera = { frontCameraLauncher.launch(null) },
                                onRetakePhoto = { frontCameraLauncher.launch(null) },
                                onClearPhoto = {
                                    frontPhotoUri = null
                                    frontPhotoBitmap = null
                                },
                                modifier = Modifier.weight(1f)
                            )

                            IdCardPhotoPickerBox(
                                title = "Mặt sau",
                                photoUri = backPhotoUri,
                                photoBitmap = backPhotoBitmap,
                                onPickPhotoFromGallery = { backPhotoLauncher.launch("image/*") },
                                onTakePhotoFromCamera = { backCameraLauncher.launch(null) },
                                onRetakePhoto = { backCameraLauncher.launch(null) },
                                onClearPhoto = {
                                    backPhotoUri = null
                                    backPhotoBitmap = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            val newCust = Customer(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                phone = phone,
                                address = address.ifBlank { "TP. Quy Nhơn, Bình Định" },
                                type = type,
                                email = email.ifBlank { "contact@example.com" },
                                idNumber = idNumber.ifBlank { "012345678901" },
                                idCardFrontPhotoUri = frontPhotoUri,
                                idCardBackPhotoUri = backPhotoUri
                            )
                            onAddCustomer(newCust)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Lưu khách hàng")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Hủy")
                }
            }
        )

        if (showQrScannerDialog) {
            CccdCameraScannerSheet(
                onDismiss = { showQrScannerDialog = false },
                onCccdScanned = { cccdData ->
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    name = cccdData.fullName
                    idNumber = cccdData.cccdNumber
                    address = cccdData.address
                    Toast.makeText(context, "Đã quét mã QR CCCD thành công!", Toast.LENGTH_SHORT).show()
                    showQrScannerDialog = false
                }
            )
        }
    }

    // Customer Detail Dialog
    if (showDetailDialog && selectedCustomer != null) {
        val cust = selectedCustomer!!
        AlertDialog(
            onDismissRequest = { showDetailDialog = false },
            icon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(36.dp)) },
            title = { Text(cust.name, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Phân loại: ${cust.type}", fontWeight = FontWeight.Medium)
                    Text("Số điện thoại: ${cust.phone}", color = PrimaryOrange, fontWeight = FontWeight.Bold)
                    if (cust.idNumber.isNotBlank()) {
                        Text("CCCD/Mã số thuế: ${cust.idNumber}", fontWeight = FontWeight.Medium)
                    }
                    Text("Địa chỉ: ${cust.address}")
                    Text("Email: ${cust.email}")

                    if (!cust.idCardFrontPhotoUri.isNullOrEmpty() || !cust.idCardBackPhotoUri.isNullOrEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Ảnh giấy tờ tùy thân:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!cust.idCardFrontPhotoUri.isNullOrEmpty()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AsyncImage(
                                        model = cust.idCardFrontPhotoUri,
                                        contentDescription = "Mặt trước",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Mặt trước", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                            if (!cust.idCardBackPhotoUri.isNullOrEmpty()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AsyncImage(
                                        model = cust.idCardBackPhotoUri,
                                        contentDescription = "Mặt sau",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Mặt sau", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDetailDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Đóng")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDetailDialog = false
                        customerToDelete = cust
                        showDeleteConfirmDialog = true
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = BorderStroke(1.dp, Color.Red),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Xóa khách hàng", fontSize = 12.sp)
                }
            }
        )
    }

    // Customer Delete Confirmation Dialog
    if (showDeleteConfirmDialog && customerToDelete != null) {
        val target = customerToDelete!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Xóa khách hàng", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa khách hàng '${target.name}' không?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCustomer(target)
                        showDeleteConfirmDialog = false
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CustomersScreenPreview() {
    NinoRentTheme {
        CustomersScreen()
    }
}
