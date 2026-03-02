# Weka SPADE (Sequential Pattern Discovery using Equivalence classes)

Repository này là phiên bản mở rộng của Weka core (`weka-dev-3.9.7-SNAPSHOT`), được tích hợp sẵn thuật toán khai thác mẫu tuần tự **SPADE** (Sequential PAttern Discovery using Equivalence classes).

Thuật toán SPADE, được đề xuất bởi Mohammed J. Zaki (2001), phân tích dữ liệu tuần tự hiệu quả qua định dạng Vertical ID-List và kỹ thuật phân lớp tương đương (Equivalence Classes). Việc tích hợp được mô phỏng theo chuẩn của các thuật toán Weka như GSP (Generalized Sequential Patterns) để đảm bảo độ tương thích hoàn toàn.

---

## 1. Tổng Quan Đơn Giản Thuật Toán SPADE 🧠

**SPADE** là một thuật toán khai phá mẫu tuần tự (Sequential Pattern Mining) vô cùng mạnh mẽ và tối ưu. Thay vì phải quét qua cơ sở dữ liệu nhiều lần để kiểm đếm như thuật toán Apriori hay GSP, SPADE sử dụng một **định dạng dữ liệu dọc (Vertical Data Format)** được gọi là **ID-Lists**.

Cụ thể, đối với mỗi một Item, thuật toán sẽ theo dõi danh sách các sự kiện mà nó xuất hiện dưới dạng một ma trận các cặp `(Sequence ID - SID, Event ID - EID)`.
Nhờ vào cấu trúc dọc này, SPADE có thể tìm ra các mẫu tuần tự kéo dài bằng cách thực hiện phép **kết nối (join)** trực tiếp trên các danh sách ID-Lists, giúp tránh phải lặp qua lại toàn bộ Data và giảm thiểu đáng kể chi phí tính toán truy xuất cũng như bộ nhớ. Hơn nữa, nó chia không gian duyệt tổng thể thành các "lớp tương đương" (Equivalence Classes) có chung tiền tố, giúp giới hạn số lượng pattern sinh ra.

---

## 2. Cách Thức Tích Hợp Vào Weka 🔌

Thuật toán SPADE không chạy dưới dạng mã rời rạc mà đã được tích hợp sâu vào hệ thống lõi của thư viện dữ liệu Weka:

- Lớp `Spade` được cho kế thừa từ `weka.associations.AbstractAssociator` - nền tảng chuẩn của tất cả thuật toán luật kết hợp/tuần tự trong Weka.
- Thuật toán triển khai các giao diện `OptionHandler` và `TechnicalInformationHandler`, giúp nó có khả năng kết nối dễ dàng vào cả **Weka GUI (Explorer/KnowledgeFlow)** lẫn công cụ dòng lệnh **Command-Line (CLI)**.
- SPADE được viết lại để truy xuất và xử lý dữ liệu trực tiếp dưới dạng đối tượng `Instances` của Weka (cấu trúc bảng/hàng), sau đó biến đổi cấu trúc này sang dạng Vertical Database để tiến hành thuật toán nguyên bản một cách trơn tru.

---

## 3. Các File Quan Trọng Được Thêm Vào 📂

Để đưa SPADE vào hoạt động thực tiễn trong Weka, cấu trúc hệ thống đã được mở rộng bằng các tệp Java cốt lõi bên trong gói thư mục `weka.associations`:

### Core Algorithm (Thuật Toán Chính):

- **`Spade.java`**: File điều phối trung tâm chứa logic cấu hình tùy chọn, thiết lập tham số Weka và thực thi luồng chuyển đổi `buildAssociations()`.
- **`spade/IdList.java`**: Cấu trúc dữ liệu danh sách dọc đặc trưng của SPADE, đảm nhiệm chức năng Join thời gian (`temporalJoin`) và Join đồng thời (`equalityJoin`).
- **`spade/Sequence.java`**: Đại diện tập hợp một dãy mẫu tuần tự được khai phá.
- **`spade/EquivalenceClass.java`**: Quản lý nhóm các mẫu có chung đoạn tiền tố (prefix) để thu nhỏ giới hạn bộ nhớ theo từng vùng.
- **`spade/Element.java`**: Hình thức lưu giữ itemset (khi nhiều items xảy ra trong cùng một Event thời điểm).

### Giao Diện Hiển Thị (Properties):

- **`GenericPropertiesCreator.props`** & **`GenericObjectEditor.props`**: Tập hợp các file hệ thống đã được cập nhật nội dung để Weka Explorer quét ra thuật toán `weka.associations.Spade` ở màn hình Frontend.

### Test Suite (JUnit 5 Unit Tests):

- `SpadeBoundaryTest.java`, `SpadeFunctionalTest.java`, `SpadeInternalAlgorithmTest.java`, `SpadePropertyTest.java`, `SpadeRegressionTest.java`, `SpadeStressTest.java`: Đây là bộ bảo đảm chất lượng (QA) hạng nặng kiểm thử các tính năng SPADE, ngăn chặn OutOfMemoryError, và dò tìm Edge-Cases (Ví dụ như Minimum Number of Instances = 0).

---

## 4. Quản Trị Dữ Liệu & Phạm Vi Tùy Chỉnh (Customization Range) ⚙️

### Định Dạng Dữ Liệu Được Hỗ Trợ:

Khác với nhiều thuật toán khai phá cơ bản, SPADE tích hợp trong Weka có khả năng xử lý nhiều luồng Data linh hoạt:

1. **Dữ Liệu Quan Hệ (Relational Attributes):** Được hỗ trợ chuẩn hóa cho chuỗi (thường dùng định dạng luồng `.arff`). Mỗi Sequence bao gồm một `Bag/Relation` các Events, và mỗi Event lại chứa nhiều hàng mục Items.
2. **Dữ Liệu Bảng Phẳng (Horizontal Attributes):** Ở góc độ đơn giản hơn, nếu Data chỉ có các dòng sự kiện, chức năng Flat sẽ tự động bắt một cột chỉ mục làm **Sequence ID** và gộp các event cùng ID lại với nhau.
3. Chấp nhận các kiểu cột thông dụng: `Nominal` (Phân loại), `String` (Chuỗi), và `Numeric` (Số liệu). Thuật toán cũng tự động bỏ qua các trường `Missing Values` trống.

### Tùy Chỉnh Tham Số (Options Range):

SPADE cho phép cấu hình can thiệp qua giao diện người dùng Weka hoặc qua arguments:

- **`-S <threshold>` (Minimum Support)**: Tỉ lệ phần trăm Threshold tối thiểu (phạm vi từ `0.0` đến `1.0`). Mặc định là `0.5` (50%). Xác định điều kiện một mẫu chuỗi phải xuất hiện trong bao nhiêu % tệp dữ liệu thì mới được kết luận là "Frequent Pattern".
- **`-I <index>` (Sequence ID Index)**: Chỉ số thứ tự cột (Bắt đầu đếm từ 1) dùng làm ID nhận diện phân cách Sequence khi bạn nhập liệu dạng Bảng Phẳng/Horizontal. Mặc định là Cột `1`.
- **`-D` (Debug Mode)**: Chế độ xuất Log Terminal, in ra các số lượng cấu trúc và độ biến thiên ID-Lists cho mục đích tối ưu phần cứng.

---

## 5. Cách Chạy SPADE Trong Thực Tế 🚀

### Cách 1: Qua giao diện Weka GUI (Explorer)

1. Mở ứng dụng **Weka GUI Chooser** bằng cách chạy file `run_weka.bat` và nhấp chọn ứng dụng **Explorer**.
2. Tại thẻ **Preprocess**: Nhấn _Open file..._ và nạp tệp dữ liệu (ARFF/CSV) có cấu trúc tuần tự hoặc có định danh cọt SeqID.
3. Chuyển sang thẻ **Associate**: Nhấp chọn nút **Choose** ở thanh thiết lập thuật toán trên cùng.
4. Mở rộng thư mục `weka` → `associations` → Chọn **Spade**.
5. Bạn có thể Click chuột trái vào thanh tên **Spade** vừa chọn để mở bảng cài đặt (Sửa đổi `minSupport`, v.v).
6. Nhấn nút **Start** ở góc phải để chạy khai phá mẫu! Các mẫu kết quả sẽ xuất hiện trên màn hình Log.

### Cách 2: Qua Command Line (CLI) Terminal

Dành cho người dùng xử lý lô (Batch processing):

```bash
java -cp weka.jar weka.associations.Spade -t data/sequence_data.arff -S 0.4 -I 1
```

### Cách 3: Kiểm thử lập trình nghiệm thu (Maven)

Cách chạy toàn bộ Test Suites của thuật toán SPADE ở môi trường lập trình để nghiệm thu:

```bash
mvn test -Dtest="weka.associations.Spade*Test"
```
