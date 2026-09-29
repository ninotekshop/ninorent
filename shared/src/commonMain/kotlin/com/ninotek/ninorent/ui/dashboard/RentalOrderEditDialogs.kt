package com.ninotek.ninorent.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ninotek.ninorent.model.RentalOrder
import com.ninotek.ninorent.ui.theme.PrimaryOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRentalOrderDialog(
    order: RentalOrder,
    onDismiss: () -> Unit,
    onConfirm: (RentalOrder) -> Unit
) {
    var customerName by remember { mutableStateOf(order.customerName) }
    var customerPhone by remember { mutableStateOf(order.lesseePhone) }
    var dateRange by remember { mutableStateOf(order.dateRange) }
    var totalAmount by remember { mutableStateOf(order.netTotal.ifBlank { order.price }) }
    var advancePayment by remember { mutableStateOf(order.advancePaymentAmount) }
    var status by remember { mutableStateOf(order.status) }
    var notes by remember { mutableStateOf(order.collateralAssetDescription) }

    val statusOptions = listOf("Đang thuê", "Đã trả", "Quá hạn")
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa đơn thuê", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Tên khách hàng") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("Số điện thoại") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateRange,
                    onValueChange = { dateRange = it },
                    label = { Text("Thời gian (VD: 19:00 20/09 - 19:00 21/09)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = totalAmount,
                    onValueChange = { totalAmount = it },
                    label = { Text("Tổng tiền") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = advancePayment,
                    onValueChange = { advancePayment = it },
                    label = { Text("Tiền cọc/Trả trước") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Trạng thái") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        statusOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    status = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Ghi chú") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedOrder = order.copy(
                        customerName = customerName,
                        lesseeName = customerName,
                        lesseePhone = customerPhone,
                        dateRange = dateRange,
                        price = totalAmount,
                        netTotal = totalAmount,
                        advancePaymentAmount = advancePayment,
                        status = status,
                        collateralAssetDescription = notes
                    )
                    onConfirm(updatedOrder)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = Color.Gray)
            }
        }
    )
}
