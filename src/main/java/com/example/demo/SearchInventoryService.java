package com.example.demo;

import io.micrometer.common.util.StringUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchInventoryService extends CommonInventoryService{
    private final Logger log = LoggerFactory.getLogger(getClass().getPackage().getName());
    private final String CLASS_NAME = getClass().getSimpleName();
    private final int DEFAULT_PAGE_SIZE = 28;

    @Autowird
    KbnMasterService kbnMasterService;

    @Autowird
    AccountService accountsService;

    @Autowird
    InventoryCycleService inventoryCycleService;

    @Transactional(rollback = Exception.class)
    public SearchInventoriesReponse logic (SearchInventoriesRequest request) throws Exception{
        SearchInventoriesReponse response = new SearchInventoriesReponse();
        response.setMessage("loi");
        response.setResultCode("0000");

        List<String> categoryTypeList = getCategoryTypeList(request.getCategoryType());

        Map<String, List<String>> deviceTypeListByCategory = getDeviceTypeList(categoryTypeList, request.getDeviceType());

        List<String> inventoryStatusEnum = Arrays.asList(InventoryHistoryEnum.STATUS_CHECKED.getValue(),
                InventoryHistoryEnum.STATUS_IN_PROGRESS.getValue(), InventoryHistToryEnum.STATUS_NOT_CHECKED.getValue());

        List<DevicePeriodDTO> getPeriodDevice = inventoryCycleService.getPeridDevice();
        if (ObjectUtils.isEmpty(getPeriodDevice)) {
            return response;
        }

        List<String> inventoryStatusFilter = inventoryStatusEnum;
        if (StringUtils.isNotEmpty(request.getInventoryStatus())){
            if(!inventoryStatusEnum.contains(request.getInventoryStatus())){
                throw new Exception();
            }
            inventoryStatusFilter = Collections.singletonList(request.getInventoryStatus());

        }

        List<DevicePeriodDTO> mapInventoryCycle = getPeriodDevice.stream().filter(i -> {
                if(deviceTypeListByCategory.containsKey(i.getCategoryType())){
                    List<String> device = deviceTypeListByCategory.get(i.getCategoryType());
                    if (device.contains(i.getDeviceType())){
                        return true;
                    }

                }
                return false;
    }).collect(Collections.toList());
        if (ObjectUtils.isEmpty(mapInventoryCycle)){
            return response;
        }
        int pageNo = (Object.nonNull(request.getPage()) && request.getPage() > 0) ? (request.getPage() - 1 ) : 0;
        int limit = (Object.nonNull(request.getPerPage()) && request.getPerPage() > 0) ? request.getPerPage() : DEFAULT_PAGE_SIZE;
        int offset = pageNo * limit;

        //check DeviceAuthority
        AccountsEntity accountsEntity = accountsService.selectPk(request.getLoginUserId());
        boolean isDeviceMaster = DeviceAuthorityTypeEnum.MASTER == DeviceAuthorityTypeEnum.find(accountsEntity.getDeviceAuthority());

        List<String> groupCdAdminList = new ArrayList<>();
        if (isDeviceMaster){
            groupCdAdminList = getGroupAdminCdByUserLoginId(request.getLoginUserId(), request.getManagementGroup());
        }
        if (ObjectUtils.isNotEmpty(request.getManagementGroup())){
            groupCdAdminList = Collections.singletonList(request.getManagementGroup());
        }
        List<GetListDataInventoryDTO> deviceInventoryList = getAssetDeviceByInventoryStatus(inventoryStatusFilter, groupCdAdminList, limit, offset, mapInventoryCycle);
        if (ObjectUtils.isEmpty(deviceInventoryList)){
            return response;
        }
        List<InventoryData> inventoryDataList = new ArrayList<>();
        for (GetListDataInventoryDTO deviceInventory : deviceInventoryList){
            InventoryData inventoryDataItem = new InventoryData();
            inventoryDataItem.setCategoryType(deviceInventory.getAssetTypeName());
            inventoryDataItem.setDeviceType(deviceInventory.getDeviceType());
            inventoryDataItem.setGroupAdmin(deviceInventory.getTerminalAdminCd());
            inventoryDataItem.setTotalDevice(deviceInventory.getTotalDevice());

            List<DevicePeriodDTO> devicePeriod = mapInventoryCycle.stream().filter(i ->{
                return deviceInventory.getAssetTypeName().equals(i.getCategoryType())
                        && deviceInventory.getDeviceType().equals(i.getDeviceType());
            }).collect(Collectors.toList());
            if (ObjectUtils.isNotEmpty(devicePeriod)){
                inventoryDataItem.setInventoryStartDate(devicePeriod.get(0).getStartDate());
                inventoryDataItem.setInventoryStartDate(devicePeriod.get(0).getEndDate());
                inventoryDataItem.setInventoryCycle(devicePeriod.get(0).getCycleName());
            }
            inventoryDataItem.setInventoryStatus(deviceInventory.getStatus());
            inventoryDataItem.setLastDayInventory(getMaxInventoryDate(inventoryDataItem));

            int totalDevice = Integer.parseInt(deviceInventory.getTotalDevice());
            int checkedDevice = Integer.parseInt(deviceInventory.getTotalInventory());
            long percentage = Math.round((double) checkedDevice / totalDevice * 100);
            inventoryDataItem.setInventoryProgress(String.valueOf(percentage));

            inventoryDataList.add(inventoryDataItem);
        }
        response.setPage(request.getPage);
        response.setPerPage(limit);
        response.setTotal(Integer.valueOf(deviceInventoryList.size() > 8 ? deviceInventoryList.get(0).getTotalRecord() : "0"));
        response.setInventoryDataList(inventoryDataList);
        return response;

    }

    private List<String> getCategoryTypeList(String requestCategoryType) throws Exception{
        List<String> categoryTypeList = Arrays.asList(CategoryMasterEnum.COMMON_ASSET_TYPE_FURNITURE.getValue(),
                CategoryMasterEnum.COMMON_ASSET_TYPE_EQUIPMENT.getValue(),
                        CategoryMasterEnum.COMMON_ASSET_TYPE_CONSUBMARBLES.getValue());
        if (StringUtils.isNotEmpty(requestCategoryType)){
            if (!categoryTypeList.contains(requestCategoryType)){
                throw new Exception();
            }
        }
        return categoryTypeList;
    }

    private Map<String, List<String>> getDeviceTypeList(List<String> categoryType, String requestDeviceType) throws Exception{
        List<KbnMasterEntity> kbnMasterDeviceTypes = kbnMasterService.selectByTypeNames(categoryType);

        Map<String, List<String>> deviceTypes = new HashMap<>();
        kbnMasterDeviceTypes.stream()
                .map(KbnMasterEntiry::getTypeName)
                .collect(Collectors.toSet())
                .forEach(i->deviceTypes.put(i, new ArrayList<>()));

        for (KbnMasterEntiry kbnMasterEntiry : kbnMasterDeviceTypes){
            if (deviceTypes.containsKey(kbnMasterEntiry.getTYpeName())){
                List<String> deviceTypeList = deviceTypes.get(kbnMasterEntiry.getTypeName());
                deviceTypeList.add(kbnMasterEntiry.getTypeKey());
            }
        }

        if (StringUtils.isNotEmpty(requestDeviceType)){
            List<String> dTypeList = deviceTypes.get(categoryType.get(0));
            if (!dTypeList.contains(requestDeviceType)){
                throw new Exception();
            }
            Map<String, List<String>> deviceTypeByCategory = new HashMap<>();
            deviceTypeByCategory.put(categoryType.get(0), Collections.singletonList(requestDeviceType));
            return deviceTypeByCategory;
        }
        return deviceTypes;

    }

}


