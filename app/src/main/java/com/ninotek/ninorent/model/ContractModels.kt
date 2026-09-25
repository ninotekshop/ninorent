package com.ninotek.ninorent.model

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.ninotek.ninorent.utils.parseCccdQrPayload
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class PaperSize(val label: String) {
    A4("Khổ A4"),
    A5("Khổ A5")
}

data class Equipment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,
    val categorySubtitle: String,
    val pricePerDay: String,
    val status: String, // "Sẵn sàng", "Đang thuê", "Tạm ngưng", "Quá hạn"
    val icon: ImageVector,
    val serialNumber: String = "",
    val storeId: String = ""
)

val defaultDevicesList = listOf(
    // 1. Máy ảnh - Sony (20 items)
    Equipment("sony-01", "Sony A350", "Máy ảnh", "Máy ảnh - Sony", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-SONY-A350", "SHOP_0901992349"),
    Equipment("sony-02", "Sony A55", "Máy ảnh", "Máy ảnh - Sony", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-SONY-A55", "SHOP_0901992349"),
    Equipment("sony-03", "Sony Nex-3", "Máy ảnh", "Máy ảnh - Sony", "150.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-SONY-NEX3", "SHOP_0901992349"),
    Equipment("sony-04", "Sony Nex-C3", "Máy ảnh", "Máy ảnh - Sony", "150.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-SONY-NEXC3", "SHOP_0901992349"),
    Equipment("sony-05", "Sony Nex-5", "Máy ảnh", "Máy ảnh - Sony", "150.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-SONY-NEX5", "SHOP_0901992349"),
    Equipment("sony-06", "Sony Nex-F3", "Máy ảnh", "Máy ảnh - Sony", "150.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-SONY-NEXF3", "SHOP_0901992349"),
    Equipment("sony-07", "Sony Nex-5N", "Máy ảnh", "Máy ảnh - Sony", "150.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-SONY-NEX5N", "SHOP_0901992349"),
    Equipment("sony-08", "Sony Nex-5T", "Máy ảnh", "Máy ảnh - Sony", "150.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-SONY-NEX5T", "SHOP_0901992349"),
    Equipment("sony-09", "Sony Nex-5R", "Máy ảnh", "Máy ảnh - Sony", "150.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-SONY-NEX5R", "SHOP_0901992349"),
    Equipment("sony-10", "Sony Nex-6", "Máy ảnh", "Máy ảnh - Sony", "150.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-SONY-NEX6", "SHOP_0901992349"),
    Equipment("sony-11", "Sony Nex-7", "Máy ảnh", "Máy ảnh - Sony", "150.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-SONY-NEX7", "SHOP_0901992349"),
    Equipment("sony-12", "Sony ZV-1", "Máy ảnh", "Máy ảnh - Sony", "250.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-SONY-ZV1", "SHOP_0901992349"),
    Equipment("sony-13", "Sony ZV-e10", "Máy ảnh", "Máy ảnh - Sony", "300.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-SONY-ZVE10", "SHOP_0901992349"),
    Equipment("sony-14", "Sony A5000", "Máy ảnh", "Máy ảnh - Sony", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-SONY-A5000", "SHOP_0901992349"),
    Equipment("sony-15", "Sony A5100", "Máy ảnh", "Máy ảnh - Sony", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-SONY-A5100", "SHOP_0901992349"),
    Equipment("sony-16", "Sony A6000", "Máy ảnh", "Máy ảnh - Sony", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-SONY-A6000", "SHOP_0901992349"),
    Equipment("sony-17", "Sony A6300", "Máy ảnh", "Máy ảnh - Sony", "300.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-SONY-A6300", "SHOP_0901992349"),
    Equipment("sony-18", "Sony A6500", "Máy ảnh", "Máy ảnh - Sony", "300.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-SONY-A6500", "SHOP_0901992349"),
    Equipment("sony-19", "Sony A6400", "Máy ảnh", "Máy ảnh - Sony", "350.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-SONY-A6400", "SHOP_0901992349"),
    Equipment("sony-20", "Sony A7II", "Máy ảnh", "Máy ảnh - Sony", "300.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-SONY-A7M2", "SHOP_0901992349"),

    // 2. Máy ảnh - Fujifilm (12 items)
    Equipment("fuji-01", "Fujifilm Instax Mini 12", "Máy ảnh", "Fujifilm - Instax", "150.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-FUJI-IN12", "SHOP_0901992349"),
    Equipment("fuji-02", "Fujifilm Instax Mini 70", "Máy ảnh", "Fujifilm - Instax", "100.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-FUJI-IN70", "SHOP_0901992349"),
    Equipment("fuji-03", "Fujifilm Instax Mini 9", "Máy ảnh", "Fujifilm - Instax", "100.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-FUJI-IN9", "SHOP_0901992349"),
    Equipment("fuji-04", "Fujifilm XA2", "Máy ảnh", "Fujifilm - XA Series", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-FUJI-XA2", "SHOP_0901992349"),
    Equipment("fuji-05", "Fujifilm XA3", "Máy ảnh", "Fujifilm - XA Series", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-FUJI-XA3", "SHOP_0901992349"),
    Equipment("fuji-06", "Fujifilm XA10", "Máy ảnh", "Fujifilm - XA Series", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-FUJI-XA10", "SHOP_0901992349"),
    Equipment("fuji-07", "Fujifilm XA5", "Máy ảnh", "Fujifilm - XA Series", "250.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-FUJI-XA5", "SHOP_0901992349"),
    Equipment("fuji-08", "Fujifilm XA7", "Máy ảnh", "Fujifilm - XA Series", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-FUJI-XA7", "SHOP_0901992349"),
    Equipment("fuji-09", "Fujifilm X-T100", "Máy ảnh", "Fujifilm - X Series Cao Cấp", "300.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-FUJI-XT100", "SHOP_0901992349"),
    Equipment("fuji-10", "Fujifilm X-T2", "Máy ảnh", "Fujifilm - X Series Cao Cấp", "350.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-FUJI-XT2", "SHOP_0901992349"),
    Equipment("fuji-11", "Fujifilm X-T200", "Máy ảnh", "Fujifilm - X Series Cao Cấp", "350.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-FUJI-XT200", "SHOP_0901992349"),
    Equipment("fuji-12", "Fujifilm X-T30", "Máy ảnh", "Fujifilm - X Series Cao Cấp", "300.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-FUJI-XT30", "SHOP_0901992349"),

    // 3. Máy ảnh - Nikon (17 items)
    Equipment("nikon-01", "Nikon J1", "Máy ảnh", "Nikon - 1 Series", "100.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-NIKON-J1", "SHOP_0901992349"),
    Equipment("nikon-02", "Nikon V2", "Máy ảnh", "Nikon - 1 Series", "100.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-NIKON-V2", "SHOP_0901992349"),
    Equipment("nikon-03", "Nikon 1 S1", "Máy ảnh", "Nikon - 1 Series", "100.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-NIKON-1S1", "SHOP_0901992349"),
    Equipment("nikon-04", "Nikon J2", "Máy ảnh", "Nikon - 1 Series", "100.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-NIKON-J2", "SHOP_0901992349"),
    Equipment("nikon-05", "Nikon D40x", "Máy ảnh", "Nikon - DSLR Phổ thông", "100.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-NIKON-D40X", "SHOP_0901992349"),
    Equipment("nikon-06", "Nikon D60", "Máy ảnh", "Nikon - DSLR Phổ thông", "120.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-NIKON-D60", "SHOP_0901992349"),
    Equipment("nikon-07", "Nikon D90", "Máy ảnh", "Nikon - DSLR Phổ thông", "150.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-NIKON-D90", "SHOP_0901992349"),
    Equipment("nikon-08", "Nikon D300", "Máy ảnh", "Nikon - DSLR Phổ thông", "150.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-NIKON-D300", "SHOP_0901992349"),
    Equipment("nikon-09", "Nikon D3200", "Máy ảnh", "Nikon - DSLR Tầm trung", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-NIKON-D3200", "SHOP_0901992349"),
    Equipment("nikon-10", "Nikon D3400", "Máy ảnh", "Nikon - DSLR Tầm trung", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-NIKON-D3400", "SHOP_0901992349"),
    Equipment("nikon-11", "Nikon D5000", "Máy ảnh", "Nikon - DSLR Tầm trung", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-NIKON-D5000", "SHOP_0901992349"),
    Equipment("nikon-12", "Nikon D5100", "Máy ảnh", "Nikon - DSLR Tầm trung", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-NIKON-D5100", "SHOP_0901992349"),
    Equipment("nikon-13", "Nikon D5300", "Máy ảnh", "Nikon - DSLR Tầm trung", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-NIKON-D5300", "SHOP_0901992349"),
    Equipment("nikon-14", "Nikon D7000", "Máy ảnh", "Nikon - DSLR Tầm trung", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-NIKON-D7000", "SHOP_0901992349"),
    Equipment("nikon-15", "Nikon D700", "Máy ảnh", "Nikon - Full-Frame Cao cấp", "250.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-NIKON-D700", "SHOP_0901992349"),
    Equipment("nikon-16", "Nikon D800", "Máy ảnh", "Nikon - Full-Frame Cao cấp", "350.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-NIKON-D800", "SHOP_0901992349"),
    Equipment("nikon-17", "Nikon D750", "Máy ảnh", "Nikon - Full-Frame Cao cấp", "400.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-NIKON-D750", "SHOP_0901992349"),

    // 4. Máy ảnh - Canon (37 items)
    Equipment("canon-01", "Canon 1000D", "Máy ảnh", "Canon - DSLR Phổ thông", "100.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-1000D", "SHOP_0901992349"),
    Equipment("canon-02", "Canon 1200D", "Máy ảnh", "Canon - DSLR Phổ thông", "100.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-1200D", "SHOP_0901992349"),
    Equipment("canon-03", "Canon 2000D", "Máy ảnh", "Canon - DSLR Phổ thông", "100.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-2000D", "SHOP_0901992349"),
    Equipment("canon-04", "Canon 100D", "Máy ảnh", "Canon - DSLR Nhỏ gọn", "250.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-100D", "SHOP_0901992349"),
    Equipment("canon-05", "Canon 200D", "Máy ảnh", "Canon - DSLR Nhỏ gọn", "250.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-200D", "SHOP_0901992349"),
    Equipment("canon-06", "Canon 550D", "Máy ảnh", "Canon - DSLR Tầm trung", "250.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-550D", "SHOP_0901992349"),
    Equipment("canon-07", "Canon 600D", "Máy ảnh", "Canon - DSLR Tầm trung", "250.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-600D", "SHOP_0901992349"),
    Equipment("canon-08", "Canon 650D", "Máy ảnh", "Canon - DSLR Tầm trung", "250.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-650D", "SHOP_0901992349"),
    Equipment("canon-09", "Canon 700D", "Máy ảnh", "Canon - DSLR Tầm trung", "250.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-700D", "SHOP_0901992349"),
    Equipment("canon-10", "Canon 750D", "Máy ảnh", "Canon - DSLR Tầm trung", "250.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-750D", "SHOP_0901992349"),
    Equipment("canon-11", "Canon 800D", "Máy ảnh", "Canon - DSLR Tầm trung", "250.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-800D", "SHOP_0901992349"),
    Equipment("canon-12", "Canon 40D", "Máy ảnh", "Canon - DSLR Bán chuyên", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-40D", "SHOP_0901992349"),
    Equipment("canon-13", "Canon 50D", "Máy ảnh", "Canon - DSLR Bán chuyên", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-50D", "SHOP_0901992349"),
    Equipment("canon-14", "Canon 60D", "Máy ảnh", "Canon - DSLR Bán chuyên", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-60D", "SHOP_0901992349"),
    Equipment("canon-15", "Canon 70D", "Máy ảnh", "Canon - DSLR Bán chuyên", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-70D", "SHOP_0901992349"),
    Equipment("canon-16", "Canon 80D", "Máy ảnh", "Canon - DSLR Bán chuyên", "250.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-80D", "SHOP_0901992349"),
    Equipment("canon-17", "Canon 5D", "Máy ảnh", "Canon - Full-Frame DSLR", "150.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-5D", "SHOP_0901992349"),
    Equipment("canon-18", "Canon 5D Mark II", "Máy ảnh", "Canon - Full-Frame DSLR", "150.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-5D2", "SHOP_0901992349"),
    Equipment("canon-19", "Canon 5D Mark III", "Máy ảnh", "Canon - Full-Frame DSLR", "150.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-5D3", "SHOP_0901992349"),
    Equipment("canon-20", "Canon 6D", "Máy ảnh", "Canon - Full-Frame DSLR", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-6D", "SHOP_0901992349"),
    Equipment("canon-21", "Canon 6D Mark II", "Máy ảnh", "Canon - Full-Frame DSLR", "250.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-6D2", "SHOP_0901992349"),
    Equipment("canon-22", "Canon 7D", "Máy ảnh", "Canon - Tốc độ cao", "150.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-7D", "SHOP_0901992349"),
    Equipment("canon-23", "Canon RP", "Máy ảnh", "Canon - Mirrorless RF", "400.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-RP", "SHOP_0901992349"),
    Equipment("canon-24", "Canon R", "Máy ảnh", "Canon - Mirrorless RF", "400.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-R", "SHOP_0901992349"),
    Equipment("canon-25", "Canon R50", "Máy ảnh", "Canon - Mirrorless RF", "400.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-R50", "SHOP_0901992349"),
    Equipment("canon-26", "Canon R6", "Máy ảnh", "Canon - Mirrorless RF", "350.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-R6", "SHOP_0901992349"),
    Equipment("canon-27", "Canon R100", "Máy ảnh", "Canon - Mirrorless RF", "350.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-R100", "SHOP_0901992349"),
    Equipment("canon-28", "Canon EOS M", "Máy ảnh", "Canon - Mirrorless EOS M", "100.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-EOSM", "SHOP_0901992349"),
    Equipment("canon-29", "Canon EOS M2", "Máy ảnh", "Canon - Mirrorless EOS M", "150.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-EOSM2", "SHOP_0901992349"),
    Equipment("canon-30", "Canon EOS M3", "Máy ảnh", "Canon - Mirrorless EOS M", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-EOSM3", "SHOP_0901992349"),
    Equipment("canon-31", "Canon EOS M5", "Máy ảnh", "Canon - Mirrorless EOS M", "250.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-EOSM5", "SHOP_0901992349"),
    Equipment("canon-32", "Canon EOS M6", "Máy ảnh", "Canon - Mirrorless EOS M", "250.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-EOSM6", "SHOP_0901992349"),
    Equipment("canon-33", "Canon M10", "Máy ảnh", "Canon - Mirrorless EOS M", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-M10", "SHOP_0901992349"),
    Equipment("canon-34", "Canon M50", "Máy ảnh", "Canon - Mirrorless EOS M", "250.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-M50", "SHOP_0901992349"),
    Equipment("canon-35", "Canon M50 Mark II", "Máy ảnh", "Canon - Mirrorless EOS M", "300.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-M50M2", "SHOP_0901992349"),
    Equipment("canon-36", "Canon M100", "Máy ảnh", "Canon - Mirrorless EOS M", "200.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-M100", "SHOP_0901992349"),
    Equipment("canon-37", "Canon M200", "Máy ảnh", "Canon - Mirrorless EOS M", "250.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-CANON-M200", "SHOP_0901992349"),

    // 5. Máy Game & Phụ kiện (10 items)
    Equipment("game-01", "Sony Playstation 4 Slim", "Máy Game & Phụ kiện", "Máy Game & Phụ kiện", "80.000", "Sẵn sàng", Icons.Rounded.SportsEsports, "SN-GAME-PS4SLIM", "SHOP_0901992349"),
    Equipment("game-02", "Sony Playstation 4 Slim Full Game", "Máy Game & Phụ kiện", "Máy Game & Phụ kiện", "120.000", "Sẵn sàng", Icons.Rounded.SportsEsports, "SN-GAME-PS4FG", "SHOP_0901992349"),
    Equipment("game-03", "Sony Playstation 5 Fat Disc", "Máy Game & Phụ kiện", "Máy Game & Phụ kiện", "150.000", "Sẵn sàng", Icons.Rounded.SportsEsports, "SN-GAME-PS5FAT", "SHOP_0901992349"),
    Equipment("game-04", "Nintendo Switch Lite Full Game", "Máy Game & Phụ kiện", "Máy Game & Phụ kiện", "80.000", "Sẵn sàng", Icons.Rounded.SportsEsports, "SN-GAME-NSWLIGHT", "SHOP_0901992349"),
    Equipment("game-05", "Nintendo Switch V2", "Máy Game & Phụ kiện", "Máy Game & Phụ kiện", "100.000", "Sẵn sàng", Icons.Rounded.SportsEsports, "SN-GAME-NSWV2", "SHOP_0901992349"),
    Equipment("game-06", "Nintendo Switch V2 Full Game", "Máy Game & Phụ kiện", "Máy Game & Phụ kiện", "120.000", "Sẵn sàng", Icons.Rounded.SportsEsports, "SN-GAME-NSWV2FG", "SHOP_0901992349"),
    Equipment("game-07", "Nintendo Switch Oled", "Máy Game & Phụ kiện", "Máy Game & Phụ kiện", "150.000", "Sẵn sàng", Icons.Rounded.SportsEsports, "SN-GAME-NSWOLED", "SHOP_0901992349"),
    Equipment("game-08", "Game Retro 4 nút", "Máy Game & Phụ kiện", "Máy Game & Phụ kiện", "60.000", "Sẵn sàng", Icons.Rounded.SportsEsports, "SN-GAME-RETRO4N", "SHOP_0901992349"),
    Equipment("game-09", "Tay cầm PS4", "Máy Game & Phụ kiện", "Máy Game & Phụ kiện", "30.000", "Sẵn sàng", Icons.Rounded.SportsEsports, "SN-GAME-TCPS4", "SHOP_0901992349"),
    Equipment("game-10", "Tay cầm PS5", "Máy Game & Phụ kiện", "Máy Game & Phụ kiện", "50.000", "Sẵn sàng", Icons.Rounded.SportsEsports, "SN-GAME-TCPS5", "SHOP_0901992349"),

    // 6. Thiết bị bổ sung (7 items)
    Equipment("cam-01", "DJI Pocket 3", "Máy quay", "Pocket / Gimbal", "150.000", "Sẵn sàng", Icons.Rounded.Videocam, "SN-DJI-POCKET3", "SHOP_0901992349"),
    Equipment("cam-02", "Action 6", "Máy quay", "Action Cam", "150.000", "Sẵn sàng", Icons.Rounded.Videocam, "SN-ACTION-6", "SHOP_0901992349"),
    Equipment("cam-03", "Gopro 11", "Máy quay", "Action Cam", "150.000", "Sẵn sàng", Icons.Rounded.Videocam, "SN-GOPRO-11", "SHOP_0901992349"),
    Equipment("cam-04", "Sony HDR-CX115E", "Máy quay", "Camcorder", "100.000", "Sẵn sàng", Icons.Rounded.Videocam, "SN-SONY-CX115E", "SHOP_0901992349"),
    Equipment("film-01", "Máy ảnh film", "Máy ảnh", "Máy ảnh Film", "100.000", "Sẵn sàng", Icons.Rounded.CameraAlt, "SN-FILM-01", "SHOP_0901992349"),
    Equipment("acc-01", "Đèn flash", "Phụ kiện", "Đèn Flash", "80.000", "Sẵn sàng", Icons.Rounded.Devices, "SN-FLASH-01", "SHOP_0901992349"),
    Equipment("acc-02", "Lens tùy chọn", "Phụ kiện", "Ống kính / Lens", "100.000", "Sẵn sàng", Icons.Rounded.Devices, "SN-LENS-01", "SHOP_0901992349")
)

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val time: String,
    val isRead: Boolean = false
)

data class Customer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val address: String,
    val type: String = "Cá nhân", // "Cá nhân" or "Công ty"
    val email: String = "",
    val idNumber: String = "",
    val idIssueDate: String = "",
    val idCardFrontPhotoUri: String? = null,
    val idCardBackPhotoUri: String? = null,
    val storeId: String = ""
)

data class LessorInfo(
    val name: String = "",
    val representative: String = "",
    val address: String = "",
    val phone: String = "",
    val logoUri: String? = null
)

val defaultLessorInfo = LessorInfo()

fun saveLessorInfoToPrefs(context: Context, info: LessorInfo) {
    val prefs = context.getSharedPreferences("ninorent_prefs", Context.MODE_PRIVATE)
    prefs.edit()
        .putString("lessor_name", info.name)
        .putString("lessor_address", info.address)
        .putString("lessor_representative", info.representative)
        .putString("lessor_phone", info.phone)
        .putString("lessor_logo_uri", info.logoUri)
        .apply()
}

fun loadLessorInfoFromPrefs(context: Context): LessorInfo {
    val prefs = context.getSharedPreferences("ninorent_prefs", Context.MODE_PRIVATE)
    return LessorInfo(
        name = prefs.getString("lessor_name", null) ?: defaultLessorInfo.name,
        address = prefs.getString("lessor_address", null) ?: defaultLessorInfo.address,
        representative = prefs.getString("lessor_representative", null) ?: defaultLessorInfo.representative,
        phone = prefs.getString("lessor_phone", null) ?: defaultLessorInfo.phone,
        logoUri = prefs.getString("lessor_logo_uri", null)
    )
}

data class BankAccountInfo(
    val bankName: String = "",
    val accountNumber: String = "",
    val accountHolderName: String = ""
)

val defaultBankAccountInfo = BankAccountInfo()

fun saveBankAccountInfoToPrefs(context: Context, info: BankAccountInfo) {
    val prefs = context.getSharedPreferences("ninorent_prefs", Context.MODE_PRIVATE)
    prefs.edit()
        .putString("bank_name", info.bankName)
        .putString("bank_account_number", info.accountNumber)
        .putString("bank_account_holder", info.accountHolderName)
        .apply()
}

fun loadBankAccountInfoFromPrefs(context: Context): BankAccountInfo {
    val prefs = context.getSharedPreferences("ninorent_prefs", Context.MODE_PRIVATE)
    return BankAccountInfo(
        bankName = prefs.getString("bank_name", null) ?: defaultBankAccountInfo.bankName,
        accountNumber = prefs.getString("bank_account_number", null) ?: defaultBankAccountInfo.accountNumber,
        accountHolderName = prefs.getString("bank_account_holder", null) ?: defaultBankAccountInfo.accountHolderName
    )
}

fun isLessorInfoConfigured(context: Context): Boolean {
    val prefs = context.getSharedPreferences("ninorent_prefs", Context.MODE_PRIVATE)
    return prefs.contains("lessor_name") && prefs.contains("lessor_phone")
}

fun isBankAccountConfigured(context: Context): Boolean {
    val prefs = context.getSharedPreferences("ninorent_prefs", Context.MODE_PRIVATE)
    return prefs.contains("bank_name") && prefs.contains("bank_account_number")
}

fun isStoreConfigured(context: Context): Boolean {
    return isLessorInfoConfigured(context) && isBankAccountConfigured(context)
}

data class ContractInfo(
    val lessorName: String = "",
    val lessorAddress: String = "",
    val lessorRepresentative: String = "",
    val lessorPhone: String = "",

    val lesseeName: String = "",
    val lesseeAddress: String = "",
    val lesseePhone: String = "",
    val lesseeIdNumber: String = "",
    val lesseeIdIssueDate: String = "",

    val advancePaymentAmount: String = "0",
    val collateralCccd: Boolean = false,
    val collateralGplx: Boolean = false,
    val collateralAssetDescription: String = "",
    val collateralCashAmount: String = "0",

    val idCardFrontPhotoUri: String? = null,
    val idCardBackPhotoUri: String? = null,

    val contractLocation: String = "Quy Nhơn",
    val contractDate: String = ""
)

data class RentalOrder(
    val id: String = UUID.randomUUID().toString(),
    val equipmentName: String,
    val dateRange: String,
    val price: String,
    val status: String, // "Đang thuê", "Đã trả", "Quá hạn"
    val customerName: String,

    val serialNumber: String = "",
    val discountAmount: String = "0",
    val netTotal: String = "",
    val equipmentItems: List<CartItem> = emptyList(),

    val bankName: String = "MB Bank",
    val bankAccountNumber: String = "0987654321",
    val bankAccountHolder: String = "Công ty TNHH Công nghệ & Dịch vụ NINOTEK",

    // Contract fields
    val lessorName: String = "Công ty TNHH Công nghệ & Dịch vụ NINOTEK",
    val lessorAddress: String = "125/2 Hai Bà Trưng, Phường Quy Nhơn, Tỉnh Gia Lai",
    val lessorRepresentative: String = "Ông Lê Trung Hiếu - Giám Đốc",
    val lessorPhone: String = "0901992349",

    val lesseeName: String = "",
    val lesseeAddress: String = "",
    val lesseePhone: String = "",
    val lesseeIdNumber: String = "",
    val lesseeIdIssueDate: String = "",

    val advancePaymentAmount: String = "0",
    val collateralCccd: Boolean = false,
    val collateralGplx: Boolean = false,
    val collateralAssetDescription: String = "",
    val collateralCashAmount: String = "0",

    val idCardFrontPhotoUri: String? = null,
    val idCardBackPhotoUri: String? = null,

    val contractLocation: String = "Quy Nhơn",
    val contractDate: String = "",
    val storeId: String = ""
)

fun parseCccdQrCode(qrData: String): ParsedCccdData? {
    val payload = parseCccdQrPayload(qrData) ?: return null
    return ParsedCccdData(
        idNumber = payload.cccdNumber,
        name = payload.fullName,
        address = payload.address,
        issueDate = payload.issueDate,
        dob = payload.dateOfBirth,
        gender = payload.gender
    )
}

data class ParsedCccdData(
    val idNumber: String,
    val name: String,
    val address: String,
    val issueDate: String,
    val dob: String = "",
    val gender: String = ""
)

data class CartItem(
    val id: String = UUID.randomUUID().toString(),
    val equipmentName: String,
    val pricePerDay: String,
    var serialNumber: String = ""
)

@Stable
class CreateRentalOrderState(
    initialDevices: List<Equipment> = emptyList()
) {
    var currentStep by mutableIntStateOf(1)
    var generatedOrderId by mutableStateOf("#DH00${(10..99).random()}")

    // Step 1: Customer Details - empty defaults per requirement 2
    var lesseeName by mutableStateOf("")
    var lesseePhone by mutableStateOf("")
    var lesseeAddress by mutableStateOf("")
    var lesseeIdNumber by mutableStateOf("")
    var lesseeIdIssueDate by mutableStateOf("")
    var idCardFrontPhotoUri by mutableStateOf<String?>(null)
    var idCardFrontPhotoBitmap by mutableStateOf<Bitmap?>(null)
    var idCardBackPhotoUri by mutableStateOf<String?>(null)
    var idCardBackPhotoBitmap by mutableStateOf<Bitmap?>(null)
    var showQrScannerDialog by mutableStateOf(false)
    var scanSuccessMessage by mutableStateOf<String?>(null)

    // Step 2: Shopping Cart & Equipment Selection
    val cartItems = mutableStateListOf<CartItem>()
    
    private val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())

    var startDate by mutableStateOf("")
    var startTime by mutableStateOf("")
    var endDate by mutableStateOf("")
    var endTime by mutableStateOf("")
    var rentalDays by mutableStateOf(1.0)

    fun updateEndDateTime() {
        if (startDate.isBlank() || startTime.isBlank()) return
        try {
            val sdfFull = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
            val date = sdfFull.parse("$startTime $startDate") ?: return
            
            val hoursToAdd = (rentalDays * 24).toLong()
            val minutesToAdd = ((rentalDays * 24 - hoursToAdd) * 60).toLong()
            
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.add(Calendar.HOUR_OF_DAY, hoursToAdd.toInt())
            calendar.add(Calendar.MINUTE, minutesToAdd.toInt())
            
            endDate = sdfDate.format(calendar.time)
            endTime = sdfTime.format(calendar.time)
        } catch (_: Exception) {}
    }

    // Discount: VNĐ vs %
    var discountTypeIsPercent by mutableStateOf(false) // false = VNĐ, true = %
    var discountValueText by mutableStateOf("0")

    // Step 3: Payment & Collateral
    var advancePaymentAmount by mutableStateOf("2.000.000")
    var paymentMethod by mutableStateOf("Chuyển khoản VietQR") // "Tiền mặt" vs "Chuyển khoản VietQR"
    var paymentConfirmed by mutableStateOf(false)
    var collateralCccd by mutableStateOf(true)
    var collateralGplx by mutableStateOf(false)
    var hasCollateralAsset by mutableStateOf(false)
    var collateralAssetDescription by mutableStateOf("Xe máy Honda Vision BKS 77F1-123.45")
    var hasCollateralCash by mutableStateOf(false)
    var collateralCashAmount by mutableStateOf("5.000.000")
    var contractLocation by mutableStateOf("TP. Quy Nhơn, Bình Định")
    var contractDate by mutableStateOf("20/09/2026")

    // Step 4: Paper Size
    var selectedPaperSize by mutableStateOf(PaperSize.A4)

    init {
        // Cart starts empty per requirement 2
        val now = Date()
        startDate = sdfDate.format(now)
        startTime = sdfTime.format(now)
        contractDate = startDate
        updateEndDateTime()
    }

    fun populateDefaultCart(devices: List<Equipment>) {
        // Left for backward compatibility if needed, but no longer populates default items
    }

    fun reset(devices: List<Equipment> = emptyList()) {
        currentStep = 1
        generatedOrderId = "#DH00${(10..99).random()}"
        lesseeName = ""
        lesseePhone = ""
        lesseeAddress = ""
        lesseeIdNumber = ""
        lesseeIdIssueDate = ""
        idCardFrontPhotoUri = null
        idCardFrontPhotoBitmap = null
        idCardBackPhotoUri = null
        idCardBackPhotoBitmap = null
        showQrScannerDialog = false
        scanSuccessMessage = null

        cartItems.clear()

        val now = Date()
        startDate = sdfDate.format(now)
        startTime = sdfTime.format(now)
        rentalDays = 1.0
        updateEndDateTime()
        
        discountTypeIsPercent = false
        discountValueText = "0"

        advancePaymentAmount = "2.000.000"
        paymentMethod = "Chuyển khoản VietQR"
        paymentConfirmed = false
        collateralCccd = true
        collateralGplx = false
        hasCollateralAsset = false
        collateralAssetDescription = "Xe máy Honda Vision BKS 77F1-123.45"
        hasCollateralCash = false
        collateralCashAmount = "5.000.000"
        contractLocation = "TP. Quy Nhơn, Bình Định"
        contractDate = startDate
        selectedPaperSize = PaperSize.A4
    }
}
