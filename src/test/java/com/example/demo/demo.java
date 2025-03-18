package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class demo {
    @InjectMocks
    private SearchInventoryService searchInventoryService;

    @Mock
    private KbnMasterService kbnMasterService;

    @Mock
    private AccountService accountsService;

    @Mock
    private InventoryCycleService inventoryCycleService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testLogic_GetPeriodDeviceIsEmpty() throws Exception {
        SearchInventoriesRequest request = new SearchInventoriesRequest();
        when(inventoryCycleService.getPeridDevice()).thenReturn(Collections.emptyList());

        SearchInventoriesReponse response = searchInventoryService.logic(request);

        assertNotNull(response);
        assertEquals("0000", response.getResultCode());
        assertEquals("loi", response.getMessage());
    }

    @Test
    public void testLogic_InvalidInventoryStatus() throws Exception {
        SearchInventoriesRequest request = new SearchInventoriesRequest();
        request.setInventoryStatus("INVALID_STATUS");
        when(inventoryCycleService.getPeridDevice()).thenReturn(Arrays.asList(new DevicePeriodDTO()));

        assertThrows(Exception.class, () -> searchInventoryService.logic(request));
    }

    @Test
    public void testLogic_ValidInventoryStatus() throws Exception {
        SearchInventoriesRequest request = new SearchInventoriesRequest();
        request.setInventoryStatus(InventoryHistoryEnum.STATUS_CHECKED.getValue());
        when(inventoryCycleService.getPeridDevice()).thenReturn(Arrays.asList(new DevicePeriodDTO()));
        when(accountsService.selectPk(anyString())).thenReturn(new AccountsEntity());
        when(inventoryCycleService.getAssetDeviceByInventoryStatus(anyList(), anyList(), anyInt(), anyInt(), anyList()))
                .thenReturn(Arrays.asList(new GetListDataInventoryDTO()));

        SearchInventoriesReponse response = searchInventoryService.logic(request);

        assertNotNull(response);
        assertEquals("0000", response.getResultCode());
        assertNotNull(response.getInventoryDataList());
    }

    @Test
    public void testGetCategoryTypeList_ValidCategoryType() throws Exception {
        String validCategoryType = CategoryMasterEnum.COMMON_ASSET_TYPE_FURNITURE.getValue();
        List<String> result = searchInventoryService.getCategoryTypeList(validCategoryType);

        assertNotNull(result);
        assertTrue(result.contains(validCategoryType));
    }

    @Test
    public void testGetCategoryTypeList_InvalidCategoryType() {
        String invalidCategoryType = "INVALID_CATEGORY";

        assertThrows(Exception.class, () -> searchInventoryService.getCategoryTypeList(invalidCategoryType));
    }

    @Test
    public void testGetDeviceTypeList_ValidDeviceType() throws Exception {
        List<String> categoryTypeList = Arrays.asList(CategoryMasterEnum.COMMON_ASSET_TYPE_FURNITURE.getValue());
        String validDeviceType = "VALID_DEVICE_TYPE";
        when(kbnMasterService.selectByTypeNames(categoryTypeList)).thenReturn(Arrays.asList(new KbnMasterEntity()));

        Map<String, List<String>> result = searchInventoryService.getDeviceTypeList(categoryTypeList, validDeviceType);

        assertNotNull(result);
        assertTrue(result.containsKey(categoryTypeList.get(0)));
    }

    @Test
    public void testGetDeviceTypeList_InvalidDeviceType() {
        List<String> categoryTypeList = Arrays.asList(CategoryMasterEnum.COMMON_ASSET_TYPE_FURNITURE.getValue());
        String invalidDeviceType = "INVALID_DEVICE_TYPE";
        when(kbnMasterService.selectByTypeNames(categoryTypeList)).thenReturn(Arrays.asList(new KbnMasterEntity()));

        assertThrows(Exception.class, () -> searchInventoryService.getDeviceTypeList(categoryTypeList, invalidDeviceType));
    }

    @Test
    public void testGetCategoryTypeList_NullOrEmptyRequestCategoryType() throws Exception {
        List<String> result1 = searchInventoryService.getCategoryTypeList(null);
        List<String> result2 = searchInventoryService.getCategoryTypeList("");

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(3, result1.size()); // Danh sách mặc định có 3 phần tử
    }

    @Test
    public void testGetCategoryTypeList_ValidRequestCategoryType() throws Exception {
        String validCategoryType = CategoryMasterEnum.COMMON_ASSET_TYPE_FURNITURE.getValue();
        List<String> result = searchInventoryService.getCategoryTypeList(validCategoryType);

        assertNotNull(result);
        assertTrue(result.contains(validCategoryType));
    }

    @Test
    public void testGetCategoryTypeList_InvalidRequestCategoryType() {
        String invalidCategoryType = "INVALID_CATEGORY_TYPE";
        assertThrows(Exception.class, () -> searchInventoryService.getCategoryTypeList(invalidCategoryType));
    }

    @Test
    public void testGetDeviceTypeList_NullOrEmptyRequestDeviceType() throws Exception {
        // Mock dữ liệu
        List<KbnMasterEntity> mockKbnMasterList = Arrays.asList(
                new KbnMasterEntity("FURNITURE", "DEVICE_TYPE_1"),
                new KbnMasterEntity("FURNITURE", "DEVICE_TYPE_2")
        );
        when(kbnMasterService.selectByTypeNames(anyList())).thenReturn(mockKbnMasterList);

        // Test
        Map<String, List<String>> result1 = searchInventoryService.getDeviceTypeList(Arrays.asList("FURNITURE"), null);
        Map<String, List<String>> result2 = searchInventoryService.getDeviceTypeList(Arrays.asList("FURNITURE"), "");

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(1, result1.size()); // Chỉ có một category (FURNITURE)
        assertEquals(2, result1.get("FURNITURE").size()); // Có 2 device types
    }

    @Test
    public void testGetDeviceTypeList_ValidRequestDeviceType() throws Exception {
        // Mock dữ liệu
        List<KbnMasterEntity> mockKbnMasterList = Arrays.asList(
                new KbnMasterEntity("FURNITURE", "DEVICE_TYPE_1"),
                new KbnMasterEntity("FURNITURE", "DEVICE_TYPE_2")
        );
        when(kbnMasterService.selectByTypeNames(anyList())).thenReturn(mockKbnMasterList);

        // Test
        String validDeviceType = "DEVICE_TYPE_1";
        Map<String, List<String>> result = searchInventoryService.getDeviceTypeList(Arrays.asList("FURNITURE"), validDeviceType);

        assertNotNull(result);
        assertEquals(1, result.size()); // Chỉ có một category (FURNITURE)
        assertEquals(1, result.get("FURNITURE").size()); // Chỉ có một device type được chọn
        assertEquals(validDeviceType, result.get("FURNITURE").get(0));
    }

    @Test
    public void testGetDeviceTypeList_InvalidRequestDeviceType() {
        // Mock dữ liệu
        List<KbnMasterEntity> mockKbnMasterList = Arrays.asList(
                new KbnMasterEntity("FURNITURE", "DEVICE_TYPE_1"),
                new KbnMasterEntity("FURNITURE", "DEVICE_TYPE_2")
        );
        when(kbnMasterService.selectByTypeNames(anyList())).thenReturn(mockKbnMasterList);

        // Test
        String invalidDeviceType = "INVALID_DEVICE_TYPE";
        assertThrows(Exception.class, () -> searchInventoryService.getDeviceTypeList(Arrays.asList("FURNITURE"), invalidDeviceType));
    }
    /// /
    @Test
    public void testLogic_GetPeriodDeviceIsEmpty() throws Exception {
        // Tạo request
        SearchInventoriesRequest request = new SearchInventoriesRequest();

        // Mock dữ liệu: getPeridDevice trả về danh sách rỗng
        when(inventoryCycleService.getPeridDevice()).thenReturn(Collections.emptyList());

        // Gọi phương thức logic
        SearchInventoriesResponse response = searchInventoryService.logic(request);

        // Kiểm tra kết quả
        assertNotNull(response);
        assertEquals("0000", response.getResultCode());
        assertEquals("loi", response.getMessage());
    }

    @Test
    public void testLogic_InvalidInventoryStatus() throws Exception {
        // Tạo request với inventoryStatus không hợp lệ
        SearchInventoriesRequest request = new SearchInventoriesRequest();
        request.setInventoryStatus("INVALID_STATUS");

        // Mock dữ liệu: getPeridDevice trả về một danh sách không rỗng
        when(inventoryCycleService.getPeridDevice()).thenReturn(Arrays.asList(new DevicePeriodDTO()));

        // Kiểm tra xem phương thức logic có ném ra ngoại lệ không
        assertThrows(Exception.class, () -> searchInventoryService.logic(request));
    }

    @Test
    public void testLogic_ValidInventoryStatus() throws Exception {
        // Tạo request với inventoryStatus hợp lệ
        SearchInventoriesRequest request = new SearchInventoriesRequest();
        request.setInventoryStatus(InventoryHistoryEnum.STATUS_CHECKED.getValue());

        // Mock dữ liệu
        when(inventoryCycleService.getPeridDevice()).thenReturn(Arrays.asList(new DevicePeriodDTO()));
        when(accountsService.selectPk(anyString())).thenReturn(new AccountsEntity());

        // Mock dữ liệu cho getAssetDeviceByInventoryStatus (nếu cần)
        // Giả sử phương thức này được gọi từ một service khác hoặc từ chính SearchInventoryService
        // Ví dụ:
        // when(assetDeviceService.getAssetDeviceByInventoryStatus(anyList(), anyList(), anyInt(), anyInt(), anyList()))
        //     .thenReturn(Arrays.asList(new GetListDataInventoryDTO()));

        // Gọi phương thức logic
        SearchInventoriesResponse response = searchInventoryService.logic(request);

        // Kiểm tra kết quả
        assertNotNull(response);
        assertEquals("0000", response.getResultCode());
        assertNotNull(response.getInventoryDataList());
    }

    @Test
    public void testLogic_EmptyDeviceInventoryList() throws Exception {
        // Tạo request
        SearchInventoriesRequest request = new SearchInventoriesRequest();

        // Mock dữ liệu
        when(inventoryCycleService.getPeridDevice()).thenReturn(Arrays.asList(new DevicePeriodDTO()));
        when(accountsService.selectPk(anyString())).thenReturn(new AccountsEntity());

        // Mock dữ liệu: getAssetDeviceByInventoryStatus trả về danh sách rỗng
        // Giả sử phương thức này được gọi từ một service khác hoặc từ chính SearchInventoryService
        // Ví dụ:
        // when(assetDeviceService.getAssetDeviceByInventoryStatus(anyList(), anyList(), anyInt(), anyInt(), anyList()))
        //     .thenReturn(Collections.emptyList());

        // Gọi phương thức logic
        SearchInventoriesResponse response = searchInventoryService.logic(request);

        // Kiểm tra kết quả
        assertNotNull(response);
        assertEquals("0000", response.getResultCode());
        assertTrue(response.getInventoryDataList().isEmpty());
    }
}