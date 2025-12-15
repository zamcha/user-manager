package vn.tqd.mobilemall.usermanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private int page;           // Trang hiện tại (VD: 1)
    private int size;           // Số lượng bản ghi trên 1 trang (VD: 10)
    private long totalElements; // Tổng số bản ghi tìm thấy trong DB
    private int totalPages;     // Tổng số trang
    private List<T> data;       // Danh sách dữ liệu thực tế (VD: List<UserResponse>)
}