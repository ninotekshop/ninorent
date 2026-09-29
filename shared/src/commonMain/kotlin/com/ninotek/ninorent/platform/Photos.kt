package com.ninotek.ninorent.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Ảnh người dùng vừa chọn/chụp.
 * @param uri đường dẫn file cục bộ (file://...) đã được sao chép vào bộ nhớ trong của app.
 * @param preview ảnh xem trước (chỉ có khi chụp bằng camera).
 */
class PickedPhoto(val uri: String, val preview: ImageBitmap?)

/** Trả về hàm mở thư viện ảnh; kết quả gọi qua [onPhoto]. File lưu với tên bắt đầu bằng [filePrefix]. */
@Composable
expect fun rememberGalleryPicker(filePrefix: String, onPhoto: (PickedPhoto) -> Unit): () -> Unit

/** Trả về hàm mở camera chụp ảnh; kết quả gọi qua [onPhoto]. */
@Composable
expect fun rememberCameraCapture(filePrefix: String, onPhoto: (PickedPhoto) -> Unit): () -> Unit
