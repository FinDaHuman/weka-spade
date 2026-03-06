# Hướng dẫn Tái tạo & Tích hợp SPADE vào Weka

Tài liệu này là "Sổ tay xây dựng" ghi lại lộ trình đem thuật toán SPADE tích hợp "Sâu" (Native) vào core của Weka 3.9.7 thay vì làm một plugin rời rạc.

## Bước 1: Khởi tạo Cấu trúc & Tùy biến `pom.xml`
- Đi vào source weka nguyên bản (`weka/trunk/weka`).
- Can thiệp file `pom.xml` để thêm các Dependency của `JUnit 5` (`junit-jupiter-api` và `junit-jupiter-engine`). Weka mặc định dùng JUnit 4, bước này rất cần để nâng cấp suite test lên môi trường hiện đại.

## Bước 2: Viết các Core Classes thuật toán
Toàn bộ mã nguồn SPADE đặt tại `src/main/java/weka/associations/`.
- Tạo package con `weka/associations/spade/`.
- Khởi tạo các thành phần cốt lõi: `Element.java`, `IdList.java`, `EquivalenceClass.java`, `Sequence.java`.
- Ở package cha, tạo `Spade.java` làm điểm tiếp tân (entry point) liên kết API Weka (`AbstractAssociator`).

## Bước 3: Đăng ký Thuật toán vào Weka GUI
Dù đã viết code xong, Weka sẽ không hiển thị Spade trên phần mềm nếu chưa "báo cáo".
- Cập nhật thư mục `src/main/java/weka/gui/`.
- Sửa đổi file `GenericObjectEditor.props` và bổ sung `weka.associations.Spade` vào dưới danh sách `weka.associations.Associator=\`. Điều này giúp hệ thống Reflection của Weka liệt kê Spade lên GUI thả xuống.

## Bước 4: Viết File Bộ Chuyển Đổi Dữ liệu (`SequenceDataConverter.java`)
Nhằm hỗ trợ cho người dùng những định dạng thân thiện hơn (JSON, horizontal CSV) không dính đến ARFF, viết một lớp tiện ích con tự động đọc regex JSON và CSV, phân tách tập hợp multi-item event và convert ngầm đối tượng thành Weka `Instances` tiêu chuẩn có 3 thuộc tính (sequenceID, eventID, item). Mở đường cho tham số `-F` trên GUI.

## Bước 5: Viết Test Suite Nghiệm Thu
Chất lượng Weka rất gắt gao. Phải tạo ít nhất 8 tệp JUnit 5 (ví dụ `SpadeRegressionTest.java`, `SpadeBoundaryTest.java`) bọc mọi khía cạnh góc cạnh nhất (edge-cases) để đảm bảo thuật toán sinh pattern không double-count, không sụp đổ bộ nhớ, luôn cho đầu ra là Deterministic, v.v. Kèm theo một file `SpadeTest.ref` chứa kết quả chuẩn phục vụ Test Tích Hợp ngược của Native Weka Associator.

## Bước 6: Đóng gói (Packaging)
Chạy lệnh `mvn clean compile` và `mvn package -DskipTests` (hoặc test) để xuất xưởng file `weka-dev-3.9.7-SNAPSHOT.jar` sẵn sàng phân phối sử dụng trên nhiều nền tảng PC.
