# Weka - SPADE Algorithm Integration

Repository này chứa bản tích hợp chính thức của thuật toán khai phá mẫu tuần tự **SPADE** (Sequential PAttern Discovery using Equivalence classes) vào lõi của hệ thống Weka (weka-dev-3.9.7-SNAPSHOT).

## 1. Giới thiệu SPADE
SPADE (đề xuất bởi M. J. Zaki, 2001) là thuật toán tối ưu để tìm các mẫu tuần tự xuất hiện thường xuyên trong tập dữ liệu. Khác với Apriori hay GSP quét cơ sở dữ liệu nhiều lần, SPADE sử dụng cấu trúc **Vertical ID-List** (bảng dọc) để tìm kiếm pattern thông qua các phép nối (join) trên các ID-List. Kỹ thuật này cùng với việc phân lô không gian tìm kiếm thành các **Equivalence Classes** giúp SPADE chạy cực kỳ nhanh và tiết kiệm bộ nhớ.

## 2. Đặc điểm Tích hợp vào Weka
Thay vì là một plugin rời, SPADE trong dự án này được viết thành một thành phần **native** của thư viện Weka (`weka.associations.Spade`), kế thừa `AbstractAssociator`.
- **Hỗ trợ Weka GUI Explorer**, đồng thời có thể chạy qua Command Line (CLI).
- Được compile chung thành file `weka-dev-*.jar`.
- Đã được đăng ký trong danh bạ `GenericObjectEditor.props` để load tự động trên giao diện đồ hoạ.

## 3. Các định dạng dữ liệu hỗ trợ
Hệ thống linh hoạt hỗ trợ 3 dạng data chính:
1. **Relational ARFF**: Dữ liệu tuần tự nguyên bản của Weka (mỗi instance chứa các instances con đại diện cho sự kiện).
2. **Flat / Horizontal ARFF**: Dữ liệu phẳng chứa cột `Sequence ID`. SPADE tự động nhóm các dòng có chung SID thành một chuỗi sự kiện.
3. **MỚI: Dữ liệu Raw (JSON / CSV)**: Thông qua tham số `-F` (hoặc cấu hình `inputFile` trên GUI), thuật toán cho phép đọc trực tiếp file `.csv` (nhiều item trên một sự kiện, cách nhau dấu phẩy) hoặc file `.json` cấu trúc lồng nhau mà không cần chuyển đổi thủ công sang ARFF.

## 4. Cách sử dụng

### Chạy qua Weka GUI:
1. **Mở Weka Explorer bằng 1 trong 2 cách sau:**
   - **Cách 1 (Khuyên dùng):** Chạy trực tiếp file `weka/trunk/weka/run_weka.bat`.
   - **Cách 2 (Lệnh Terminal):**
     ```bash
     cd weka/trunk/weka
     java -cp "dist\weka-dev-3.9.7-SNAPSHOT-new.jar;lib\*" weka.gui.GUIChooser
     ```
2. Mở Weka Explorer, load một file `.arff` (thậm chí là file dummy) ở tab *Preprocess* để mở khóa tab *Associate*.
3. Sang tab *Associate*, chọn thuật toán `weka.associations.Spade`.
3. Nhấp vào dòng chữ Spade để mở bảng tuỳ chỉnh (GenericObjectEditor).
4. Sửa `minSupport` theo nhu cầu. Nếu muốn load file CSV/JSON, click vào mục `inputFile` và browse đến file dấy.
5. Nhấn Start.

### Chạy qua Command Line (CLI):
```bash
# Phân tích file ARFF phẳng với cột 1 là Sequence ID, support 60%
java -cp dist/weka-dev-3.9.7-SNAPSHOT.jar weka.Run weka.associations.Spade -t data.arff -S 0.6 -I 1

# Phân tích file JSON/CSV trực tiếp bằng tham số -F
java -cp dist/weka-dev-3.9.7-SNAPSHOT.jar weka.Run weka.associations.Spade -t dummy.arff -F data.json -S 0.6
```

## Xem thêm
- [Phân tích chi tiết Spade.java](spade_main_class.md)
- [Hướng dẫn tái thiết lập vào Weka](spade_reproduction_guide.md)
- [Hướng dẫn bộ chạy Test](spade_test_files.md)
