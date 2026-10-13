package com.insipidknight.toarrive;

import com.insipidknight.toarrive.common.exception.ErrorCode;
import com.insipidknight.toarrive.order.dto.OrderCreateDTO;
import com.insipidknight.toarrive.order.entity.Order;
import com.insipidknight.toarrive.order.entity.enums.CancelBy;
import com.insipidknight.toarrive.order.entity.enums.OrderStatus;
import com.insipidknight.toarrive.order.evaluation.dto.EvaluationCreateDTO;
import com.insipidknight.toarrive.order.mapper.OrderMapper;
import com.insipidknight.toarrive.order.service.OrderService;
import com.insipidknight.toarrive.user.dto.ChangePasswordDTO;
import com.insipidknight.toarrive.user.dto.DriverApplyDTO;
import com.insipidknight.toarrive.user.dto.LoginDTO;
import com.insipidknight.toarrive.user.dto.RegisterDTO;
import com.insipidknight.toarrive.user.dto.UpdateProfileDTO;
import com.insipidknight.toarrive.user.entity.enums.Gender;
import com.insipidknight.toarrive.user.entity.enums.UserRole;
import com.insipidknight.toarrive.user.mapper.AuthMapper;
import com.insipidknight.toarrive.wallet.dto.RechargeDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 端到端集成测试：走真实 Spring 上下文 + MySQL/Redis（由 application-dev 配置提供），
 * 全程经 HTTP 层（MockMvc）调用。类级 @Transactional 使每个测试方法结束后回滚，不污染库。
 * 前提：MySQL 与 Redis 已启动，DB_PASSWORD / JWT_DEV_SECRET 已注入环境变量。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiFlowIntegrationTest {

    private static final String PWD = "pass123456";
    private static final String ID_CARD = "110101199003071234";
    /** 抢单池按司机坐标检索，坐标取与 orderDto 起点一致，保证订单落在半径内。 */
    private static final String PENDING_PATH = "/api/order/pending?lng=116.397&lat=39.908";

    private static final AtomicInteger SEQ =
            new AtomicInteger((int) (System.currentTimeMillis() % 100_000_000));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private AuthMapper authMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("注册登录后可下单，并在 /mine 看到该订单")
    void register_login_createOrder_listMine() throws Exception {
        String phone = nextPhone();
        register(phone);
        String token = login(phone);

        JsonNode created = assertOk(post("/api/order", orderDto(), token));
        assertThat(created.path("data").path("status").asString()).isEqualTo("PENDING");
        String orderNo = created.path("data").path("orderNo").asString();

        JsonNode mine = assertOk(get("/api/order/mine", token));
        assertThat(mine.path("data").findValuesAsString("orderNo")).contains(orderNo);
    }

    @Test
    @DisplayName("司机入驻：管理员审核通过后，司机需重新登录才拿到 DRIVER 角色")
    void driverApply_approve_roleRefreshedOnRelogin() throws Exception {
        String phone = nextPhone();
        long userId = register(phone);
        String staleToken = login(phone);
        assertThat(roleOf(phone)).isEqualTo("PASSENGER");

        assertOk(post("/api/driver/apply", applyDto(phone), staleToken));

        String admin = newAdminToken();
        JsonNode pending = assertOk(get("/api/admin/driver/pending?limit=50", admin));
        assertThat(pending.path("data").findValuesAsString("driverId")).contains(String.valueOf(userId));

        assertThat(get("/api/admin/driver/pending", staleToken).path("code").asInt())
                .as("非管理员访问待审列表")
                .isEqualTo(ErrorCode.FORBIDDEN.getCode());

        assertOk(post("/api/admin/driver/" + userId + "/approve", null, admin));
        assertThat(post("/api/admin/driver/" + userId + "/approve", null, admin).path("code").asInt())
                .as("重复审核被拦截")
                .isEqualTo(ErrorCode.BUSINESS_ERROR.getCode());

        assertThat(get(PENDING_PATH, staleToken).path("code").asInt())
                .as("旧 token 仍是乘客角色，司机端点应 403")
                .isEqualTo(ErrorCode.FORBIDDEN.getCode());

        assertThat(roleOf(phone)).isEqualTo("DRIVER");
        assertOk(get(PENDING_PATH, login(phone)));
    }

    @Test
    @DisplayName("订单全生命周期：待接单→已接单→已到达→行程中→已完成，且时间线逐段落库")
    void orderLifecycle() throws Exception {
        String passengerPhone = nextPhone();
        register(passengerPhone);
        String orderNo = createOrder(login(passengerPhone));

        String driver = newApprovedDriverToken();
        assertThat(assertOk(get(PENDING_PATH, driver)).path("data").findValuesAsString("orderNo"))
                .contains(orderNo);

        JsonNode accepted = assertOk(post("/api/order/" + orderNo + "/accept", null, driver));
        assertThat(accepted.path("data").path("status").asString()).isEqualTo("ACCEPTED");
        assertThat(accepted.path("data").path("driverInfo").path("maskedPhone").asString())
                .as("接单后 driverInfo 应被填充")
                .isNotBlank();

        assertThat(assertOk(post("/api/order/" + orderNo + "/arrive", null, driver))
                .path("data").path("status").asString()).isEqualTo("ARRIVED");
        assertThat(assertOk(post("/api/order/" + orderNo + "/start", null, driver))
                .path("data").path("status").asString()).isEqualTo("IN_TRIP");

        JsonNode finished = assertOk(post("/api/order/" + orderNo + "/finish", null, driver));
        JsonNode data = finished.path("data");
        assertThat(data.path("status").asString()).isEqualTo("FINISHED");
        for (String field : List.of("acceptedAt", "arrivedAt", "startedAt", "finishedAt")) {
            assertThat(data.path(field).asString()).as(field).isNotBlank();
        }
        assertThat(data.path("distance").asDouble()).as("结算里程已落库").isGreaterThan(0);
        assertThat(data.path("duration").asInt()).as("结算时长已落库").isGreaterThan(0);
        assertThat(data.path("actualPrice").asDouble()).as("实付金额已落库").isGreaterThan(0);
    }

    @Test
    @DisplayName("支付：订单完结后充值 → 支付成功并扣减余额，重复支付被拦截")
    void orderPayment() throws Exception {
        String passengerPhone = nextPhone();
        register(passengerPhone);
        String passenger = login(passengerPhone);
        String orderNo = createOrder(passenger);

        String driver = newApprovedDriverToken();
        assertOk(post("/api/order/" + orderNo + "/accept", null, driver));
        assertOk(post("/api/order/" + orderNo + "/arrive", null, driver));
        assertOk(post("/api/order/" + orderNo + "/start", null, driver));
        JsonNode finished = assertOk(post("/api/order/" + orderNo + "/finish", null, driver));
        double price = finished.path("data").path("actualPrice").asDouble();
        assertThat(price).as("结束时应已结算实际金额").isGreaterThan(0);
        assertThat(finished.path("data").path("payStatus").asString()).isEqualTo("UNPAID");

        assertOk(post("/api/wallet/recharge", rechargeDto("100.00"), passenger));
        assertThat(assertOk(get("/api/wallet", passenger)).path("data").path("balance").asDouble())
                .isEqualTo(100.00);

        JsonNode paid = assertOk(post("/api/order/" + orderNo + "/pay", null, passenger));
        assertThat(paid.path("data").path("payStatus").asString()).isEqualTo("PAID");
        assertThat(paid.path("data").path("paidAt").asString()).isNotBlank();

        assertThat(post("/api/order/" + orderNo + "/pay", null, passenger).path("code").asInt())
                .as("重复支付被拦截")
                .isEqualTo(ErrorCode.BUSINESS_ERROR.getCode());

        assertThat(assertOk(get("/api/wallet", passenger)).path("data").path("balance").asDouble())
                .as("余额按实付金额扣减")
                .isCloseTo(100.00 - price, within(0.01));
    }

    @Test
    @DisplayName("支付：钱包余额不足 → 支付失败")
    void paymentInsufficientBalance() throws Exception {
        String passengerPhone = nextPhone();
        register(passengerPhone);
        String passenger = login(passengerPhone);
        String orderNo = createOrder(passenger);

        String driver = newApprovedDriverToken();
        assertOk(post("/api/order/" + orderNo + "/accept", null, driver));
        assertOk(post("/api/order/" + orderNo + "/arrive", null, driver));
        assertOk(post("/api/order/" + orderNo + "/start", null, driver));
        assertOk(post("/api/order/" + orderNo + "/finish", null, driver));

        JsonNode failed = post("/api/order/" + orderNo + "/pay", null, passenger);
        assertThat(failed.path("code").asInt()).as("钱包为空，应余额不足").isEqualTo(ErrorCode.BUSINESS_ERROR.getCode());
        assertThat(failed.path("message").asString()).contains("余额不足");
    }

    @Test
    @DisplayName("取消语义：乘客取消记 PASSENGER、司机取消记 DRIVER、行程中不可取消")
    void cancelSemantics() throws Exception {
        String passengerPhone = nextPhone();
        register(passengerPhone);
        String passenger = login(passengerPhone);

        JsonNode byPassenger = assertOk(cancel(createOrder(passenger), "临时有事", passenger));
        assertThat(byPassenger.path("data").path("status").asString()).isEqualTo("CANCELLED");
        assertThat(byPassenger.path("data").path("cancelBy").asString()).isEqualTo("PASSENGER");

        String driver = newApprovedDriverToken();

        String inTrip = createOrder(passenger);
        assertOk(post("/api/order/" + inTrip + "/accept", null, driver));
        assertOk(post("/api/order/" + inTrip + "/arrive", null, driver));
        assertOk(post("/api/order/" + inTrip + "/start", null, driver));
        assertThat(cancel(inTrip, "不想走了", passenger).path("code").asInt())
                .as("行程中乘客取消应被状态机拒绝")
                .isEqualTo(ErrorCode.BUSINESS_ERROR.getCode());
        assertOk(post("/api/order/" + inTrip + "/finish", null, driver));

        String byDriver = createOrder(passenger);
        assertOk(post("/api/order/" + byDriver + "/accept", null, driver));
        JsonNode cancelled = assertOk(cancel(byDriver, "车辆故障", driver));
        assertThat(cancelled.path("data").path("status").asString()).isEqualTo("CANCELLED");
        assertThat(cancelled.path("data").path("cancelBy").asString()).isEqualTo("DRIVER");
    }

    @Test
    @DisplayName("负例：乘客不能接单、司机单活跃订单约束、驳回必须填原因")
    void negatives() throws Exception {
        String passengerPhone = nextPhone();
        register(passengerPhone);
        String passenger = login(passengerPhone);
        String order = createOrder(passenger);

        assertThat(post("/api/order/" + order + "/accept", null, passenger).path("code").asInt())
                .as("乘客调用司机端点")
                .isEqualTo(ErrorCode.FORBIDDEN.getCode());

        String driver = newApprovedDriverToken();
        assertOk(post("/api/order/" + order + "/accept", null, driver));

        String otherPhone = nextPhone();
        register(otherPhone);
        String otherOrder = createOrder(login(otherPhone));
        assertThat(post("/api/order/" + otherOrder + "/accept", null, driver).path("code").asInt())
                .as("司机已有进行中订单，不能再接")
                .isEqualTo(ErrorCode.BUSINESS_ERROR.getCode());

        String applicantPhone = nextPhone();
        register(applicantPhone);
        String applicant = login(applicantPhone);
        assertOk(post("/api/driver/apply", applyDto(applicantPhone), applicant));
        long applicantId = userIdOf(applicantPhone);
        String admin = newAdminToken();

        assertThat(post("/api/admin/driver/" + applicantId + "/reject", null, admin).path("code").asInt())
                .as("驳回未填原因")
                .isEqualTo(ErrorCode.BUSINESS_ERROR.getCode());
        assertOk(send(MockMvcRequestBuilders.post("/api/admin/driver/" + applicantId + "/reject")
                .param("reason", "证件不清晰"), null, admin));
        assertThat(roleOf(applicantPhone)).as("被驳回后角色不变").isEqualTo("PASSENGER");
    }

    @Test
    @DisplayName("资料：更新昵称/性别后，GET 返回最新值")
    void updateProfile() throws Exception {
        String phone = nextPhone();
        register(phone);
        String token = login(phone);

        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setNickname("小骑士");
        dto.setGender(Gender.FEMALE);
        JsonNode updated = assertOk(send(MockMvcRequestBuilders.put("/api/user/profile"), dto, token));
        assertThat(updated.path("data").path("nickname").asString()).isEqualTo("小骑士");
        assertThat(updated.path("data").path("gender").asString()).isEqualTo("FEMALE");

        JsonNode got = assertOk(get("/api/user/profile", token));
        assertThat(got.path("data").path("nickname").asString()).isEqualTo("小骑士");
        assertThat(got.path("data").path("maskedPhone").asString()).as("手机号应脱敏").isNotBlank();
    }

    @Test
    @DisplayName("改密码：旧密码登录失败，新密码登录成功")
    void changePassword() throws Exception {
        String phone = nextPhone();
        register(phone);
        String token = login(phone);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword(PWD);
        dto.setNewPassword("newpass789");
        assertOk(post("/api/auth/password", dto, token));

        assertThat(loginRaw(phone).path("code").asInt())
                .as("旧密码登录应失败")
                .isEqualTo(ErrorCode.BUSINESS_ERROR.getCode());

        LoginDTO newLogin = new LoginDTO();
        newLogin.setPhone(phone);
        newLogin.setPassword("newpass789");
        assertThat(assertOk(post("/api/auth/login", newLogin, null)).path("data").path("accessToken").asString())
                .isNotBlank();
    }

    @Test
    @DisplayName("管理员用户管理：筛选 + 禁用后登录被拒 + 启用后恢复；非管理员 403")
    void adminUserManagement() throws Exception {
        String phone = nextPhone();
        long userId = register(phone);
        String admin = newAdminToken();

        assertThat(get("/api/admin/user?phone=" + phone, login(phone)).path("code").asInt())
                .as("非管理员访问用户列表")
                .isEqualTo(ErrorCode.FORBIDDEN.getCode());

        JsonNode list = assertOk(get("/api/admin/user?phone=" + phone, admin));
        assertThat(list.path("data").path("total").asLong()).isEqualTo(1);
        assertThat(list.path("data").path("list").findValuesAsString("userId")).contains(String.valueOf(userId));

        assertOk(post("/api/admin/user/" + userId + "/disable", null, admin));
        assertThat(loginRaw(phone).path("code").asInt())
                .as("禁用后登录被拒")
                .isEqualTo(ErrorCode.ACCOUNT_LOCKED.getCode());

        assertOk(post("/api/admin/user/" + userId + "/enable", null, admin));
        assertThat(loginNode(phone).path("data").path("accessToken").asString()).isNotBlank();
    }

    @Test
    @DisplayName("评价：已完成且已支付后可评一次；未支付不可评、非本人不可评、重复评价被拦")
    void evaluateOrder() throws Exception {
        String passengerPhone = nextPhone();
        register(passengerPhone);
        String passenger = login(passengerPhone);
        String orderNo = createOrder(passenger);

        String driver = newApprovedDriverToken();
        assertOk(post("/api/order/" + orderNo + "/accept", null, driver));
        assertOk(post("/api/order/" + orderNo + "/arrive", null, driver));
        assertOk(post("/api/order/" + orderNo + "/start", null, driver));
        assertOk(post("/api/order/" + orderNo + "/finish", null, driver));

        EvaluationCreateDTO eval = new EvaluationCreateDTO();
        eval.setScore(5);
        eval.setComment("很好");
        eval.setTags("准时,态度好");

        assertThat(post("/api/order/" + orderNo + "/evaluate", eval, passenger).path("code").asInt())
                .as("未支付不可评价")
                .isEqualTo(ErrorCode.BUSINESS_ERROR.getCode());

        assertOk(post("/api/wallet/recharge", rechargeDto("100.00"), passenger));
        assertOk(post("/api/order/" + orderNo + "/pay", null, passenger));

        String otherPhone = nextPhone();
        register(otherPhone);
        assertThat(post("/api/order/" + orderNo + "/evaluate", eval, login(otherPhone)).path("code").asInt())
                .as("非订单乘客不可评价")
                .isEqualTo(ErrorCode.FORBIDDEN.getCode());

        JsonNode ok = assertOk(post("/api/order/" + orderNo + "/evaluate", eval, passenger));
        assertThat(ok.path("data").path("score").asInt()).isEqualTo(5);
        assertThat(ok.path("data").path("comment").asString()).isEqualTo("很好");

        assertThat(post("/api/order/" + orderNo + "/evaluate", eval, passenger).path("code").asInt())
                .as("重复评价被拦截")
                .isEqualTo(ErrorCode.BUSINESS_ERROR.getCode());
    }

    @Test
    @DisplayName("超时取消：待接单订单超过窗口被系统取消，cancelBy=SYSTEM")
    void orderTimeoutCancel() throws Exception {
        String passengerPhone = nextPhone();
        register(passengerPhone);
        String orderNo = createOrder(login(passengerPhone));

        // 把创建时间改到 1 小时前，模拟长时间无人接单
        jdbcTemplate.update("UPDATE orders SET created_at = NOW() - INTERVAL 1 HOUR WHERE order_no = ?", orderNo);

        orderService.cancelTimeoutOrders(LocalDateTime.now().minusMinutes(10));

        Order order = orderMapper.selectByOrderNo(orderNo);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelBy()).isEqualTo(CancelBy.SYSTEM);
    }

    // ---------- HTTP helpers ----------

    private JsonNode send(MockHttpServletRequestBuilder request, Object body, String token) throws Exception {
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(body));
        }
        if (token != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        String json = mockMvc.perform(request).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return jsonMapper.readTree(json);
    }

    private JsonNode post(String path, Object body, String token) throws Exception {
        return send(MockMvcRequestBuilders.post(path), body, token);
    }

    private JsonNode get(String path, String token) throws Exception {
        return send(MockMvcRequestBuilders.get(path), null, token);
    }

    private JsonNode cancel(String orderNo, String reason, String token) throws Exception {
        return send(MockMvcRequestBuilders.post("/api/order/" + orderNo + "/cancel").param("reason", reason), null, token);
    }

    private JsonNode assertOk(JsonNode response) {
        assertThat(response.path("code").asInt()).as("响应: %s", response).isEqualTo(200);
        return response;
    }

    // ---------- 领域 helpers ----------

    private String nextPhone() {
        return "13" + String.format("%09d", SEQ.incrementAndGet());
    }

    private long register(String phone) throws Exception {
        RegisterDTO dto = new RegisterDTO();
        dto.setPhone(phone);
        dto.setPassword(PWD);
        dto.setConfirmPassword(PWD);
        return Long.parseLong(assertOk(post("/api/auth/register", dto, null)).path("data").path("userId").asString());
    }

    private String login(String phone) throws Exception {
        return loginNode(phone).path("data").path("accessToken").asString();
    }

    private String roleOf(String phone) throws Exception {
        return loginNode(phone).path("data").path("user").path("role").asString();
    }

    private JsonNode loginNode(String phone) throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setPhone(phone);
        dto.setPassword(PWD);
        return assertOk(post("/api/auth/login", dto, null));
    }

    /** 登录但不校验成功（用于断言失败场景，如改密后旧密码、账号被禁用）。 */
    private JsonNode loginRaw(String phone) throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setPhone(phone);
        dto.setPassword(PWD);
        return post("/api/auth/login", dto, null);
    }

    private long userIdOf(String phone) {
        return authMapper.selectByPhone(phone).getUserId();
    }

    private String newAdminToken() throws Exception {
        String phone = nextPhone();
        register(phone);
        authMapper.updateRole(userIdOf(phone), UserRole.ADMIN.getValue());
        return login(phone);
    }

    /** 注册 → 申请 → 管理员审核通过 → 重新登录 → 出车，返回可接单的 DRIVER token。 */
    private String newApprovedDriverToken() throws Exception {
        String phone = nextPhone();
        register(phone);
        String pendingToken = login(phone);
        assertOk(post("/api/driver/apply", applyDto(phone), pendingToken));
        assertOk(post("/api/admin/driver/" + userIdOf(phone) + "/approve", null, newAdminToken()));
        String token = login(phone);
        assertOk(post("/api/driver/online", null, token));
        return token;
    }

    private String createOrder(String token) throws Exception {
        return assertOk(post("/api/order", orderDto(), token)).path("data").path("orderNo").asString();
    }

    private static OrderCreateDTO orderDto() {
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setStartLng(116.397);
        dto.setStartLat(39.908);
        dto.setStartAddress("起点");
        dto.setEndLng(116.407);
        dto.setEndLat(39.918);
        dto.setEndAddress("终点");
        return dto;
    }

    private static RechargeDTO rechargeDto(String amount) {
        RechargeDTO dto = new RechargeDTO();
        dto.setAmount(new java.math.BigDecimal(amount));
        return dto;
    }

    private static DriverApplyDTO applyDto(String phone) {
        DriverApplyDTO dto = new DriverApplyDTO();
        dto.setRealName("测试司机");
        dto.setIdCardNo(ID_CARD);
        dto.setLicenseNo("DL" + phone);
        dto.setPlateNumber("PLATE" + phone);
        return dto;
    }
}
