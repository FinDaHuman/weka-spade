# Spade.java — Class Trung Tâm

`Spade.java` là class chính trị giá hàng trăm dòng code, đóng vai trò là cầu nối giữa bộ logic của thuật toán SPADE và hệ sinh thái của phần mềm Weka. Bằng cách kế thừa `AbstractAssociator`, nó tự động tương thích với khung pipeline của Weka.

## 1. Vai trò và Cấu trúc
Thuật toán SPADE bao gồm rất nhiều thành phần:
- `Spade.java`: Quản lý logic chính (tham số, parsing, flow khai phá).
- `IdList.java`: Bảng theo dõi vị trí xuất hiện của mẫu (chứa danh sách cặp `SID, EID`).
- `Sequence.java`: Chuỗi các sự kiện tuần tự được khai phá.
- `EquivalenceClass.java`: Phân nhóm các Sequence có chung tiền tố nhằm tối ưu không gian tìm kiếm.
- `Element.java`: Bao bọc một hoặc nhiều item diễn ra cùng một lúc.
- `SequenceDataConverter.java`: Class tiện ích mới dùng để tự động đọc và parsing CSV/JSON file.

## 2. Phân tích Flow `buildAssociations()`
Tham số chính của Weka Associator là hàm `buildAssociations()`. Hoạt động như sau:

**Bước 1: Tải dữ liệu & Check Capabilities**
Nếu tham số `-F` (file path) được cung cấp, nó sẽ bỏ qua đối tượng `Instances data` mà Weka truyền vào, tiến hành dùng `SequenceDataConverter` load lại tập dữ liệu từ CSV/JSON thành định dạng phẳng (flat data). Sau đó kiểm tra data có thoả mãn capability không (nominal attributes, rel attributes).

**Bước 2: Xây dựng Vertical Database (`buildVerticalDB`)**
Duyệt qua các format:
- *Basket Format*: Tự động dò tìm. Nếu không tìm thấy cột có tính chất ID (vd toàn bộ là nominal binary như 0/1), nó hiểu mỗi dòng là 1 chuỗi, các giá trị 1 là các item xảy ra cùng lúc (EID=0).
- *Flat Format*: Các dữ liệu có ID rõ ràng. Quét từng dòng, lấy cột làm `SID`. Các giá trị item trong cùng EID. Hàm auto skip các thuộc tính boolean `0/false`.
- *Relational Format*: Dạng bảng lồng bảng gốc của Weka.

**Bước 3: Tìm 1-sequences (Độ dài 1)**
Lọc ra các Sequence chỉ có 1 Item mà thoả mãn ngưỡng `minSupportCount`.

**Bước 4: Nối tạo 2-sequences và Group thành Equivalence Classes**
Tạo ra các phép Temporal Join (nối theo thời gian: `A -> B` và `B -> A`), và Equality Join (nối đồng thời: `{A, B}`) để lập ra các mẫu kích thước 2. Đưa các mẫu chia sẻ tiền tố vào chung `EquivalenceClass`.

**Bước 5: Đệ quy ngầm**
Các Equivalence Class tự động đối chiếu các thành viên bên trong nó để đẻ ra các mẫu kích thước 3, 4, v.v., đệ quy cho tới khi không sinh thêm mẫu nào nữa hoặc chạm `m_MaxPatternLength`.

## 3. Xử lý Options (Cấu hình GUI & CLI)
Hỗ trợ đầy đủ các tham số Weka. Các tuỳ chỉnh như `minSupport` (-S), `dataSeqID` (-I), hay `inputFile` (-F) đều có getter/setter và các string mô tả `TipText`. Cụ thể với `inputFile`, thẻ annotation `@FilePropertyMetadata` được dùng để Weka nhận diện và kết xuất thành hộp thoại chọn tệp UI đàng hoàng khi người dùng chạy Weka Explorer.
