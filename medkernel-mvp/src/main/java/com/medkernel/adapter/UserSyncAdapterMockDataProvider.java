package com.medkernel.adapter;

import com.medkernel.terminology.TerminologyService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class UserSyncAdapterMockDataProvider extends AbstractAdapterMockDataProvider {

    public UserSyncAdapterMockDataProvider(TerminologyService terminologyService) {
        super(terminologyService);
    }

    @Override
    public boolean supports(String adapterCode, String queryCode) {
        if ("HIS_ADAPTER".equals(adapterCode) && "QUERY_HIS_USERS".equals(queryCode)) return true;
        if ("EMR_ADAPTER".equals(adapterCode) && "QUERY_EMR_USERS".equals(queryCode)) return true;
        if ("OA_ADAPTER".equals(adapterCode) && "QUERY_OA_USERS".equals(queryCode)) return true;
        return false;
    }

    @Override
    public List<Map<String, Object>> provideRows(String adapterCode, String queryCode, Map<String, Object> params) {
        if ("HIS_ADAPTER".equals(adapterCode) && "QUERY_HIS_USERS".equals(queryCode)) {
            return hisUsers(params);
        }
        if ("EMR_ADAPTER".equals(adapterCode) && "QUERY_EMR_USERS".equals(queryCode)) {
            return emrUsers(params);
        }
        if ("OA_ADAPTER".equals(adapterCode) && "QUERY_OA_USERS".equals(queryCode)) {
            return oaUsers(params);
        }
        throw new UnsupportedOperationException("unsupported: " + adapterCode + "/" + queryCode);
    }

    // SEC-006: 用户同步适配器Mock数据生成方法
    private List<Map<String, Object>> hisUsers(Map<String, Object> params) {
        List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
        // 用户1
        Map<String, Object> user1 = new LinkedHashMap<String, Object>();
        user1.put("user_id", "HIS_USER_001");
        user1.put("user_name", "zhangsan");
        user1.put("display_name", "张三");
        user1.put("department_code", "DEPT_CARDIO");
        user1.put("department_name", "心血管内科");
        user1.put("position", "主任医师");
        user1.put("phone", "13800138001");
        user1.put("email", "zhangsan@hospital.com");
        user1.put("status", "ACTIVE");
        user1.put("hire_date", "2015-06-01");
        users.add(user1);
        // 用户2
        Map<String, Object> user2 = new LinkedHashMap<String, Object>();
        user2.put("user_id", "HIS_USER_002");
        user2.put("user_name", "lisi");
        user2.put("display_name", "李四");
        user2.put("department_code", "DEPT_EMERGENCY");
        user2.put("department_name", "急诊科");
        user2.put("position", "副主任医师");
        user2.put("phone", "13800138002");
        user2.put("email", "lisi@hospital.com");
        user2.put("status", "ACTIVE");
        user2.put("hire_date", "2018-03-15");
        users.add(user2);
        // 用户3
        Map<String, Object> user3 = new LinkedHashMap<String, Object>();
        user3.put("user_id", "HIS_USER_003");
        user3.put("user_name", "wangwu");
        user3.put("display_name", "王五");
        user3.put("department_code", "DEPT_RADIOLOGY");
        user3.put("department_name", "放射科");
        user3.put("position", "主治医师");
        user3.put("phone", "13800138003");
        user3.put("email", "wangwu@hospital.com");
        user3.put("status", "INACTIVE");
        user3.put("hire_date", "2020-09-01");
        users.add(user3);
        return users;
    }

    private List<Map<String, Object>> emrUsers(Map<String, Object> params) {
        List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
        // EMR用户1
        Map<String, Object> user1 = new LinkedHashMap<String, Object>();
        user1.put("user_id", "EMR_USER_001");
        user1.put("user_name", "zhangsan");
        user1.put("display_name", "张三");
        user1.put("department_code", "DEPT_CARDIO");
        user1.put("department_name", "心血管内科");
        user1.put("position", "主任医师");
        user1.put("phone", "13800138001");
        user1.put("email", "zhangsan@hospital.com");
        user1.put("status", "ACTIVE");
        users.add(user1);
        // EMR用户2
        Map<String, Object> user2 = new LinkedHashMap<String, Object>();
        user2.put("user_id", "EMR_USER_004");
        user2.put("user_name", "zhaoliu");
        user2.put("display_name", "赵六");
        user2.put("department_code", "DEPT_NURSING");
        user2.put("department_name", "护理部");
        user2.put("position", "护士长");
        user2.put("phone", "13800138004");
        user2.put("email", "zhaoliu@hospital.com");
        user2.put("status", "ACTIVE");
        users.add(user2);
        return users;
    }

    private List<Map<String, Object>> oaUsers(Map<String, Object> params) {
        List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
        // OA用户1
        Map<String, Object> user1 = new LinkedHashMap<String, Object>();
        user1.put("user_id", "OA_USER_001");
        user1.put("user_name", "zhangsan");
        user1.put("display_name", "张三");
        user1.put("department_code", "DEPT_CARDIO");
        user1.put("department_name", "心血管内科");
        user1.put("position", "主任医师");
        user1.put("phone", "13800138001");
        user1.put("email", "zhangsan@hospital.com");
        user1.put("status", "ACTIVE");
        users.add(user1);
        // OA用户2
        Map<String, Object> user2 = new LinkedHashMap<String, Object>();
        user2.put("user_id", "OA_USER_005");
        user2.put("user_name", "sunqi");
        user2.put("display_name", "孙七");
        user2.put("department_code", "DEPT_ADMIN");
        user2.put("department_name", "院办");
        user2.put("position", "行政人员");
        user2.put("phone", "13800138005");
        user2.put("email", "sunqi@hospital.com");
        user2.put("status", "ACTIVE");
        users.add(user2);
        // OA用户3
        Map<String, Object> user3 = new LinkedHashMap<String, Object>();
        user3.put("user_id", "OA_USER_006");
        user3.put("user_name", "zhouba");
        user3.put("display_name", "周八");
        user3.put("department_code", "DEPT_FINANCE");
        user3.put("department_name", "财务科");
        user3.put("position", "会计");
        user3.put("phone", "13800138006");
        user3.put("email", "zhouba@hospital.com");
        user3.put("status", "INACTIVE");
        users.add(user3);
        return users;
    }
}
