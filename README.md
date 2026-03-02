# Weka - SPADE Algorithm Integration

Repository này là phiên bản mở rộng của Weka core (`weka-dev-3.9.7-SNAPSHOT`), được tích hợp sẵn thuật toán khai thác mẫu tuần tự **SPADE** (Sequential PAttern Discovery using Equivalence classes).

Thuật toán SPADE, được đề xuất bởi Mohammed J. Zaki (2001), phân tích dữ liệu tuần tự hiệu quả qua định dạng Vertical ID-List và kỹ thuật phân lớp tương đương (Equivalence Classes). Việc tích hợp được mô phỏng theo chuẩn của các thuật toán Weka như GSP (Generalized Sequential Patterns) để đảm bảo độ tương thích hoàn toàn.

---

## 1. Các File Được Thay Đổi / Thêm Mới So Với Weka Gốc

Việc tích hợp chủ yếu tạo ra cấu trúc mã nguồn ở package mới `weka.associations.spade` và cập nhật đăng ký class trong các Properties của GUI.

### Các File Thêm Mới ([NEW])

1. `src/main/java/weka/associations/Spade.java`
2. `src/main/java/weka/associations/spade/IdList.java`
3. `src/main/java/weka/associations/spade/Element.java`
4. `src/main/java/weka/associations/spade/Sequence.java`
5. `src/main/java/weka/associations/spade/EquivalenceClass.java`
6. `src/test/java/weka/associations/SpadeTest.java`
7. `src/main/java/weka/associations/spade/sample_sequential.arff` (File dữ liệu mẫu)

### Các File Chỉnh Sửa ([MODIFIED])

1. `src/main/java/weka/gui/GenericObjectEditor.props`

---

## 2. Giải Thích Chi Tiết Các File / Class Quan Trọng

### 2.1. `weka.associations.Spade`
Class chính thực thi thuật toán SPADE, tích hợp sâu vào Weka bằng cách kế thừa `AbstractAssociator` và implement `OptionHandler`.
* **Vai trò:** Là điểm đầu vào khi người dùng gọi SPADE từ Weka GUI hoặc CLI. 
* **Quá trình thực thi (`buildAssociations`):**
   1. Đọc dữ liệu đầu vào (Instances) dạng Data Ngang (Horizontal).
   2. Chuyển đổi thành Vertical DB (sử dụng thư viện `IdList.java`).
   3. Tìm kiếm các Frequent 1-sequences và 2-sequences đầu tiên.
   4. Tạo ra các nhóm danh mục tương đương (`EquivalenceClass`).
   5. Đệ quy gọi `enumerateFrequentSequences()` để duyệt tìm các mẫu phức tạp hơn.
* **Tham số GUI:** Hỗ trợ tham số `MinSupport` (ngưỡng count tối thiểu, mặc định 0.5) và `DataSeqID` (vị trí côt Sequence ID, mặc định 1).

### 2.2. `IdList.java`
* **Vai trò:** Lõi cấu trúc dữ liệu của định dạng dọc (Vertical ID-List). Mỗi Item (vd: `item1=A`) lưu một bảng danh sách các cặp (Sequence ID, Event ID).
* **Các toán tử then chốt:**
   * `temporalJoin`: Nối hai id-list để sinh chuỗi tuần tự mới (Sequence extension - khi item xảy ra SAU item trước).
   * `equalityJoin`: Nối hai id-list để sinh Tập mục mới (Itemset extension - khi item xảy ra CÙNG LÚC với item trước trong cùng Event ID).

### 2.3. `Sequence.java` & `Element.java`
* `Element.java`: Tương đương một **Itemset**. Đây là tập hợp của 1 hoặc nhiều Items xuất hiện trong cùng 1 sự kiện thời gian.
* `Sequence.java`: Tương đương một **Chuỗi**. Cấu trúc là một mảng thứ tự (List) các `Element`. Mỗi chuỗi cũng lưu kèm 1 biến chứa đối tượng `IdList` tương ứng với chuỗi cấu trúc đó.

### 2.4. `EquivalenceClass.java`
* **Vai trò:** Thể hiện Cây Khônng Gian Duyệt Lattice theo hướng phân lớp các chuỗi có **Cùng Tiền Tố (Prefix)**.
* Khi hai thuộc tính chuỗi có chung một prefix mẹ, chúng ta chỉ cần chạy tổ hợp Nối (Join `IdList`) giữa chúng chứ không phải mang rà quét lại database ban đầu. Đây là hàm lõi đảm nhận logic đệ quy tìm kiếm DFS.

### 2.5. `GenericObjectEditor.props`
* **Sự thay đổi:** File cơ bản của Weka giữ danh sách liệt kê các Class để hiển thị trên thanh thả xuống Dropdown ở giao diện Window. Đoạn mã `weka.associations.Spade` đã được thêm vào nhóm biến `weka.associations.Associator=\`.

---

## 3. Tổng Quan Đơn Giản Thuật Toán SPADE

SPADE (M. Zaki, 2001) giải quyết bài toán khai thác chuỗi bằng cách khắc phục sự nặng nề trong I/O đĩa dính kèm theo phương pháp quen thuộc ở trước đó như Apriori All hay GSP. 

Trong khi GSP duyệt Database *Horizontal* qua nhiều vòng (đếm, băm, sinh chuỗi), thì SPADE áp dụng các kỹ thuật cốt lõi:

1. **Từ Ngang Sang Dọc (Vertical Data Representation):**
   * SPADE không quét từng dòng hóa đơn của khách. SPADE chọn lưu cho **Mỗi Item** một "bảng danh sách ID" (`IdList`) với dạng là `(Khách hàng ID, Giờ Mua ID)`.
   * **Lợi ích:** Ta chỉ cần **Join** hai bảng danh sách của "Sữa" và "Bánh Mì" là biết chính xác bao nhiêu Khách Hàng mua 2 thứ này. (Giao tập ID lại với nhau trong RAM). Thao tác này chỉ cần cộng và tính giao các phần tử nhanh chóng, không dính líu đến quét Đĩa (I/O) tốn tài nguyên.

2. **Lớp Tương Đương (Equivalence Classes):**
   * SPADE gộp các chuỗi sinh ra theo Tiền Tố (Prefix) thành những Lớp Lớn (Class).
   * Ví dụ, nhóm `[A]` sẽ chứa các chuỗi `[A->B], [A->C], [A->D]`. Nhóm `[B]` sẽ chứa `[B->C], [B->D]`.
   * **Lợi ích:** Không gian duyệt sẽ độc lập hoàn toàn với nhau. Ta có thể quăng nhóm `[A]` sang xử lý ở Thread hay Mem khác mà không cần lo bị trùng.

3. **Chỉ Quét DB Đúng Lần Đầu:**
   * Sau khi bước 1 trích xuất xong các IdList ứng với tập độ sâu L = 1, Thuật toán đã "nhét" được toàn bộ Database vào ID-List. Mọi mức độ tăng tiến về sau được truy xuất từ RAM nhờ Join Array. Thuật toán không bao giờ cần Load lại bảng Instance gốc.

---

## 4. Hướng Dẫn Chạy Thử SPADE Trên Weka

Đoạn lệnh Build Maven chuẩn đã chạy thành công và tạo file Jar đóng gói ở `/dist`.

**Cách dùng thử trực quan SPADE qua giao diện Weka:**

Chạy thư viện ở terminal:
```bash
# Vào đường dẫn project weka core
cd weka/trunk/weka

# Load thư viện bằng PowerShell và Khởi động Weka
$cp = "dist\weka-dev-3.9.7-SNAPSHOT.jar"; Get-ChildItem lib\*.jar | ForEach-Object { $cp += ";lib\$($_.Name)" }; java -cp $cp weka.gui.GUIChooser
```

1. Ở giao diện `Chooser`, chọn nút **Explorer**.
2. Ở tab `Preprocess` bấm **Open file...**, chọn File sample `.arff` (vd file `sample_sequential.arff` ở đường dẫn `src/main/java/weka/associations/spade/sample_sequential.arff`).
3. Chuyển qua tab **Associate** → Ở ô Associator (thường đang là `Apriori`), click vô nút **Choose** → Cuộn xuống chọn **Spade**.
4. Chọn xong, bấm vô "chữ (Textbox)" của thuật Toán để cấu hình tham số như `minSupport` hay `dataSeqID`. Bấm **Start** để khai thác kết quả!
