# Sổ Tay Các Tệp Kiểm Thử (SPADE Test Suite)

Thuật toán SPADE trong kho chứa này được bảo vệ bởi một mạng lưới kiểm thử (`JUnit 5`) vô cùng nghiêm ngặt, bám sát các tiêu chuẩn sản xuất. Các tệp test được lưu trữ tại `src/test/java/weka/associations/`. Có tổng cộng 8 file Test Java và 1 file REF định dạng đối chiếu.

## 1. Phân Loại Theo Mục Đích

- **`SpadeTest.java` (Weka Integration Test)**: Kế thừa từ `AbstractAssociatorTest` (JUnit 4) cũ của Weka. Được thiết kế để chạy nghiệm thu tổng quan Serialization/Deserialization (Lưu và Nạp Mô Hình máy học). Dùng tham chiếu file mẫu `SpadeTest.ref`.
- **`SpadeFunctionalTest.java` (Kiểm thử chức năng)**: Bơm các sequences giả lập đơn giản nhất và test xem các tập frequent patterns (1-sequence, 2-sequence) được in ra có chuẩn xác về support count không. (Cũng chứa class nội bộ `SpadeTestUtils` tái sử dụng dữ liệu).
- **`SpadeBoundaryTest.java` (Kiểm thử vùng biên)**: Failsafe kiểm tra thuật toán xử lý ra sao nếu Data rỗng, Data chỉ có duy nhất 1 Item, ngưỡng minSupport = 0, hoặc minSupport = 100%. Đảm bảo chương trình trả về rỗng hợp lý và không Crash hệ thống.
- **`SpadeInternalAlgorithmTest.java` (Kiểm thử Lõi Thuật Toán)**: Tập trung vào `IdList`. Verify các kịch bản khó khi nối temporal (trước sau thời gian) bị nhập nhằng với equality join (xuất hiện cùng một lúc). Giám sát xem thuật toán có bị nhầm itemset thành sequence không.
- **`SpadeDeterminismTest.java` (Kiểm thử Tính Độc Tôn Output)**: Shuffle (trộn ngẫu nhiên) thứ tự dữ liệu đầu vào. SPADE bắt buộc phải sort mọi thứ và sản sinh ra cùng một String output giống y thệt 100% trong tất cả các lần (Kể cả chạy 10 vòng lặp). Rất cần thiết đê chống lại tính nondeterministic của `HashMap`.
- **`SpadePropertyTest.java` (Kiểm thử Toán Học Anti-Monotonicity)**: Dựa trên định lý cốt lõi của Pattern Mining: Sự phổ biến của một tập hợp chuỗi con phải LỚN HƠN HOẶC BẰNG tập hợp chứa nó. Test liên tục quét xem SPADE có sinh ra luật nào tự mâu thuẫn không.
- **`SpadeRegressionTest.java` (Kiểm thử Hồi Quy Lỗi)**: Đội phá lỗi bảo trì (Regression). Khi ta từng sửa 1 bug (ví dụ Weka từng đọc lầm cột `SeqID` thành một giá trị mua hàng, làm hỏng toán học, hoặc bug auto-detect file Horizontal Basket JSON), ta viết một test ở đây để cảnh báo nếu ai đó refactor làm bug này quay lại.
- **`SpadeStressTest.java` (Kiểm thử Ngoại Vi Áp Lực)**: Bơm data dày đặc lặp lại siêu sâu (Dense Data) hoặc nạp 1000 chuỗi rời rạc. Ép rào chắn `maxPatternLength` ngăn tình trạng sập RAM (Out Of Memory) trong JVM.

## 2. Cách Vận Hành Kiểm Thử
Sử dụng Maven để chạy nguyên bộ test. Do SPADE có các Annotation của JUnit 5 trong Weka JUnit 4 base, maven-surefire plugin sẽ điều phối thông qua engine Jupiter đã được config.
```bash
mvn test -pl . -Dtest=*Spade*
```
Thoả mãn đủ bộ này chứng tỏ thuật toán đã sẵn sàng đem lên làm lõi (Gate) production.
