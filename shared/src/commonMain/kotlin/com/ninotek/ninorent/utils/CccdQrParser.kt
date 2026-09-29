package com.ninotek.ninorent.utils

/**
 * Data class đại diện cho thông tin trích xuất từ mã QR Code trên thẻ CCCD gắn chip Việt Nam.
 *
 * @property cccdNumber Số căn cước công dân 12 chữ số
 * @property oldCmndNumber Số CMND 9 chữ số cũ (có thể rỗng nếu không có)
 * @property fullName Họ và tên đầy đủ
 * @property dateOfBirth Ngày tháng năm sinh (đã định dạng DD/MM/YYYY)
 * @property gender Giới tính (Nam / Nữ)
 * @property address Địa chỉ thường trú đầy đủ
 * @property issueDate Ngày cấp thẻ CCCD (đã định dạng DD/MM/YYYY)
 */
data class CccdData(
    val cccdNumber: String,
    val oldCmndNumber: String,
    val fullName: String,
    val dateOfBirth: String,
    val gender: String,
    val address: String,
    val issueDate: String
)

/**
 * Phân tích chuỗi thô (raw payload) quét được từ QR Code thẻ CCCD.
 *
 * Cấu trúc chuỗi QR CCCD gắn chip gồm 7 trường phân cách bởi ký tự '|':
 * - Trường 0: Số CCCD 12 số (Ví dụ: "052095001234")
 * - Trường 1: Số CMND 9 số cũ (Ví dụ: "123456789")
 * - Trường 2: Họ và tên chủ thẻ (Ví dụ: "Nguyễn Văn Nam")
 * - Trường 3: Ngày sinh dạng DDMMYYYY (Ví dụ: "15051995" -> "15/05/1995")
 * - Trường 4: Giới tính (Ví dụ: "Nam" hoặc "Nữ")
 * - Trường 5: Địa chỉ thường trú đầy đủ (Ví dụ: "123 Lê Lợi, TP. Quy Nhơn, Tỉnh Bình Định")
 * - Trường 6: Ngày cấp CCCD dạng DDMMYYYY (Ví dụ: "15052021" -> "15/05/2021")
 *
 * @param rawText Chuỗi văn bản thô đọc từ camera scanner
 * @return Đối tượng [CccdData] nếu hợp lệ, hoặc `null` nếu chuỗi không đủ 7 trường hoặc rỗng
 */
fun parseCccdQrPayload(rawText: String): CccdData? {
    if (rawText.isBlank()) return null

    val parts = rawText.split("|")
    if (parts.size < 7) return null

    // Trường 0: Trích xuất số CCCD 12 số
    val cccdNumber = parts[0].trim()

    // Trường 1: Trích xuất số CMND 9 số cũ (nếu có)
    val oldCmndNumber = parts[1].trim()

    // Trường 2: Trích xuất họ và tên chủ hộ/chủ thẻ
    val fullName = parts[2].trim()

    // Trường 3: Trích xuất ngày sinh dạng thô (DDMMYYYY)
    val rawDob = parts[3].trim()

    // Trường 4: Trích xuất giới tính (Nam / Nữ)
    val gender = parts[4].trim()

    // Trường 5: Trích xuất địa chỉ thường trú
    val address = parts[5].trim()

    // Trường 6: Trích xuất ngày cấp CCCD dạng thô (DDMMYYYY)
    val rawIssueDate = parts[6].trim()

    // Kiểm tra dữ liệu bắt buộc không được rỗng
    if (cccdNumber.isEmpty() || fullName.isEmpty()) return null

    return CccdData(
        cccdNumber = cccdNumber,
        oldCmndNumber = oldCmndNumber,
        fullName = fullName,
        dateOfBirth = formatCccdDate(rawDob),
        gender = gender,
        address = address,
        issueDate = formatCccdDate(rawIssueDate)
    )
}

/**
 * Chuyển đổi định dạng ngày từ dạng thô DDMMYYYY (8 chữ số) sang dạng hiển thị chuẩn DD/MM/YYYY.
 *
 * Ví dụ: "15051995" -> "15/05/1995"
 */
private fun formatCccdDate(rawDate: String): String {
    return if (rawDate.length == 8 && rawDate.all { it.isDigit() }) {
        "${rawDate.substring(0, 2)}/${rawDate.substring(2, 4)}/${rawDate.substring(4, 8)}"
    } else {
        rawDate
    }
}
