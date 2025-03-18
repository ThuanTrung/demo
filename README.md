package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchInventoryServiceTest {
    @InjectMocks
    private SearchInventoryService searchInventoryService;

    @Mock
    private KbnMasterService kbnMasterService;

    @Mock
    private AccountService accountsService;

    @Mock
    private InventoryCycleService inventoryCycleService;

    @Test
    public void testGetCategoryTypeList_NullOrEmptyRequestCategoryType() throws Exception {
        // Trường hợp 1: requestCategoryType là null hoặc rỗng
        List<String> result1 = searchInventoryService.getCategoryTypeList(null);
        List<String> result2 = searchInventoryService.getCategoryTypeList("");

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(3, result1.size()); // Danh sách mặc định có 3 phần tử
    }

    @Test
    public void testGetCategoryTypeList_ValidRequestCategoryType() throws Exception {
        // Trường hợp 2: requestCategoryType hợp lệ
        String validCategoryType = CategoryMasterEnum.COMMON_ASSET_TYPE_FURNITURE.getValue();
        List<String> result = searchInventoryService.getCategoryTypeList(validCategoryType);

        assertNotNull(result);
        assertTrue(result.contains(validCategoryType));
    }

    @Test
    public void testGetCategoryTypeList_InvalidRequestCategoryType() {
        // Trường hợp 3: requestCategoryType không hợp lệ
        String invalidCategoryType = "INVALID_CATEGORY_TYPE";
        assertThrows(Exception.class, () -> searchInventoryService.getCategoryTypeList(invalidCategoryType));
    }

    @Test
    public void testGetDeviceTypeList_NullOrEmptyRequestDeviceType() throws Exception {
        // Trường hợp 1: requestDeviceType là null hoặc rỗng
        List<KbnMasterEntity> mockKbnMasterList = Arrays.asList(
                new KbnMasterEntity("FURNITURE", "DEVICE_TYPE_1"),
                new KbnMasterEntity("FURNITURE", "DEVICE_TYPE_2")
        );
        when(kbnMasterService.selectByTypeNames(anyList())).thenReturn(mockKbnMasterList);

        Map<String, List<String>> result1 = searchInventoryService.getDeviceTypeList(Arrays.asList("FURNITURE"), null);
        Map<String, List<String>> result2 = searchInventoryService.getDeviceTypeList(Arrays.asList("FURNITURE"), "");

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(1, result1.size()); // Chỉ có một category (FURNITURE)
        assertEquals(2, result1.get("FURNITURE").size()); // Có 2 device types
    }

    @Test
    public void testGetDeviceTypeList_ValidRequestDeviceType() throws Exception {
        // Trường hợp 2: requestDeviceType hợp lệ
        List<KbnMasterEntity> mockKbnMasterList = Arrays.asList(
                new KbnMasterEntity("FURNITURE", "DEVICE_TYPE_1"),
                new KbnMasterEntity("FURNITURE", "DEVICE_TYPE_2")
        );
        when(kbnMasterService.selectByTypeNames(anyList())).thenReturn(mockKbnMasterList);

        String validDeviceType = "DEVICE_TYPE_1";
        Map<String, List<String>> result = searchInventoryService.getDeviceTypeList(Arrays.asList("FURNITURE"), validDeviceType);

        assertNotNull(result);
        assertEquals(1, result.size()); // Chỉ có một category (FURNITURE)
        assertEquals(1, result.get("FURNITURE").size()); // Chỉ có một device type được chọn
        assertEquals(validDeviceType, result.get("FURNITURE").get(0));
    }

    @Test
    public void testGetDeviceTypeList_InvalidRequestDeviceType() {
        // Trường hợp 3: requestDeviceType không hợp lệ
        List<KbnMasterEntity> mockKbnMasterList = Arrays.asList(
                new KbnMasterEntity("FURNITURE", "DEVICE_TYPE_1"),
                new KbnMasterEntity("FURNITURE", "DEVICE_TYPE_2")
        );
        when(kbnMasterService.selectByTypeNames(anyList())).thenReturn(mockKbnMasterList);

        String invalidDeviceType = "INVALID_DEVICE_TYPE";
        assertThrows(Exception.class, () -> searchInventoryService.getDeviceTypeList(Arrays.asList("FURNITURE"), invalidDeviceType));
    }

    @Test
    public void testGetDeviceTypeList_EmptyKbnMasterList() throws Exception {
        // Trường hợp 4: kbnMasterService.selectByTypeNames trả về danh sách rỗng
        when(kbnMasterService.selectByTypeNames(anyList())).thenReturn(Collections.emptyList());

        Map<String, List<String>> result = searchInventoryService.getDeviceTypeList(Arrays.asList("FURNITURE"), null);

        assertNotNull(result);
        assertTrue(result.isEmpty()); // Kết quả trả về phải là một Map rỗng
    }

    @Test
    public void testLogic_GetPeriodDeviceIsEmpty() throws Exception {
        // Trường hợp 1: getPeridDevice trả về danh sách rỗng
        SearchInventoriesRequest request = new SearchInventoriesRequest();
        when(inventoryCycleService.getPeridDevice()).thenReturn(Collections.emptyList());

        SearchInventoriesResponse response = searchInventoryService.logic(request);

        assertNotNull(response);
        assertEquals("0000", response.getResultCode());
        assertEquals("loi", response.getMessage());
    }

    @Test
    public void testLogic_InvalidInventoryStatus() throws Exception {
        // Trường hợp 2: request.getInventoryStatus() không hợp lệ
        SearchInventoriesRequest request = new SearchInventoriesRequest();
        request.setInventoryStatus("INVALID_STATUS");
        when(inventoryCycleService.getPeridDevice()).thenReturn(Arrays.asList(new DevicePeriodDTO()));

        assertThrows(Exception.class, () -> searchInventoryService.logic(request));
    }

    @Test
    public void testLogic_ValidInventoryStatus() throws Exception {
        // Trường hợp 3: request.getInventoryStatus() hợp lệ
        SearchInventoriesRequest request = new SearchInventoriesRequest();
        request.setInventoryStatus(InventoryHistoryEnum.STATUS_CHECKED.getValue());
        when(inventoryCycleService.getPeridDevice()).thenReturn(Arrays.asList(new DevicePeriodDTO()));
        when(accountsService.selectPk(anyString())).thenReturn(new AccountsEntity());

        // Mock dữ liệu cho getAssetDeviceByInventoryStatus (nếu cần)
        // Giả sử phương thức này được gọi từ một service khác hoặc từ chính SearchInventoryService
        // Ví dụ:
        // when(assetDeviceService.getAssetDeviceByInventoryStatus(anyList(), anyList(), anyInt(), anyInt(), anyList()))
        //     .thenReturn(Arrays.asList(new GetListDataInventoryDTO()));

        SearchInventoriesResponse response = searchInventoryService.logic(request);

        assertNotNull(response);
        assertEquals("0000", response.getResultCode());
        assertNotNull(response.getInventoryDataList());
    }

    @Test
    public void testLogic_EmptyDeviceInventoryList() throws Exception {
        // Trường hợp 10: getAssetDeviceByInventoryStatus trả về danh sách rỗng
        SearchInventoriesRequest request = new SearchInventoriesRequest();
        when(inventoryCycleService.getPeridDevice()).thenReturn(Arrays.asList(new DevicePeriodDTO()));
        when(accountsService.selectPk(anyString())).thenReturn(new AccountsEntity());

        // Mock dữ liệu: getAssetDeviceByInventoryStatus trả về danh sách rỗng
        // Giả sử phương thức này được gọi từ một service khác hoặc từ chính SearchInventoryService
        // Ví dụ:
        // when(assetDeviceService.getAssetDeviceByInventoryStatus(anyList(), anyList(), anyInt(), anyInt(), anyList()))
        //     .thenReturn(Collections.emptyList());

        SearchInventoriesResponse response = searchInventoryService.logic(request);

        assertNotNull(response);
        assertEquals("0000", response.getResultCode());
        assertTrue(response.getInventoryDataList().isEmpty());
    }
}
