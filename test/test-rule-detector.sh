#!/bin/bash
# 测试用户名/密码规则检测器

echo "===== 用户名/密码规则检测器测试 ====="
echo ""

# 编译工具类
cd /root/jc-test/backend

echo "1. 编译UsernamePasswordRuleDetector..."
javac -d target/test-classes \
    -cp "$(find ~/.m2/repository/org/jsoup -name '*.jar' | tr '\n' ':')$(find ~/.m2/repository/org/slf4j -name '*.jar' | tr '\n' ':')target/classes" \
    src/main/java/com/detection/platform/utils/UsernamePasswordRuleDetector.java 2>&1

if [ $? -eq 0 ]; then
    echo "✅ 编译成功"
else
    echo "❌ 编译失败"
    exit 1
fi

echo ""
echo "2. 创建简单测试程序..."

cat > /tmp/TestRuleDetector.java <<'EOF'
import com.detection.platform.utils.UsernamePasswordRuleDetector;
import com.detection.platform.utils.UsernamePasswordRuleDetector.*;

public class TestRuleDetector {
    public static void main(String[] args) {
        System.out.println("========== 测试用户名生成 ==========");
        
        // 测试1：基本规则（6-10位，字母开头，无下划线）
        UsernameRule rule1 = new UsernameRule();
        rule1.setMinLength(6);
        rule1.setMaxLength(10);
        rule1.setMustStartWithLetter(true);
        rule1.setAllowUnderscore(false);
        
        System.out.println("\n测试1: 6-10位，字母开头，无下划线");
        for (int i = 0; i < 10; i++) {
            String username = UsernamePasswordRuleDetector.generateUsername(rule1);
            System.out.println("  生成 " + (i+1) + ": " + username + " (长度:" + username.length() + ")");
            
            // 验证
            if (username.length() < 6 || username.length() > 10) {
                System.out.println("  ❌ 长度错误！");
                System.exit(1);
            }
            if (!Character.isLetter(username.charAt(0))) {
                System.out.println("  ❌ 第一位不是字母！");
                System.exit(1);
            }
            if (username.contains("_")) {
                System.out.println("  ❌ 包含下划线！");
                System.exit(1);
            }
        }
        System.out.println("✅ 测试1通过");
        
        // 测试2：严格规则（5-8位）
        UsernameRule rule2 = new UsernameRule();
        rule2.setMinLength(5);
        rule2.setMaxLength(8);
        
        System.out.println("\n测试2: 5-8位用户名");
        for (int i = 0; i < 5; i++) {
            String username = UsernamePasswordRuleDetector.generateUsername(rule2);
            System.out.println("  生成 " + (i+1) + ": " + username + " (长度:" + username.length() + ")");
            
            if (username.length() < 5 || username.length() > 8) {
                System.out.println("  ❌ 长度错误！");
                System.exit(1);
            }
        }
        System.out.println("✅ 测试2通过");
        
        // 测试3：宽松规则（7-11位，允许下划线）
        UsernameRule rule3 = new UsernameRule();
        rule3.setMinLength(7);
        rule3.setMaxLength(11);
        rule3.setAllowUnderscore(true);
        
        System.out.println("\n测试3: 7-11位，允许下划线");
        for (int i = 0; i < 5; i++) {
            String username = UsernamePasswordRuleDetector.generateUsername(rule3);
            System.out.println("  生成 " + (i+1) + ": " + username + " (长度:" + username.length() + ")");
            
            if (username.length() < 7 || username.length() > 11) {
                System.out.println("  ❌ 长度错误！");
                System.exit(1);
            }
        }
        System.out.println("✅ 测试3通过");
        
        System.out.println("\n========== 测试密码生成 ==========");
        
        // 测试4：复杂密码规则
        PasswordRule pwdRule1 = new PasswordRule();
        pwdRule1.setMinLength(10);
        pwdRule1.setMaxLength(16);
        pwdRule1.setRequireLowerCase(true);
        pwdRule1.setRequireUpperCase(true);
        pwdRule1.setRequireDigit(true);
        pwdRule1.setRequireSpecialChar(true);
        
        System.out.println("\n测试4: 复杂密码（10-16位，大小写+数字+特殊字符）");
        for (int i = 0; i < 5; i++) {
            String password = UsernamePasswordRuleDetector.generatePassword(pwdRule1);
            System.out.println("  生成 " + (i+1) + ": " + password + " (长度:" + password.length() + ")");
            
            if (password.length() < 10 || password.length() > 16) {
                System.out.println("  ❌ 长度错误！");
                System.exit(1);
            }
            if (!password.matches(".*[a-z].*")) {
                System.out.println("  ❌ 缺少小写字母！");
                System.exit(1);
            }
            if (!password.matches(".*[A-Z].*")) {
                System.out.println("  ❌ 缺少大写字母！");
                System.exit(1);
            }
            if (!password.matches(".*\\d.*")) {
                System.out.println("  ❌ 缺少数字！");
                System.exit(1);
            }
        }
        System.out.println("✅ 测试4通过");
        
        // 测试5：简单密码
        PasswordRule pwdRule2 = new PasswordRule();
        pwdRule2.setMinLength(8);
        pwdRule2.setMaxLength(12);
        
        System.out.println("\n测试5: 简单密码（8-12位）");
        for (int i = 0; i < 5; i++) {
            String password = UsernamePasswordRuleDetector.generatePassword(pwdRule2);
            System.out.println("  生成 " + (i+1) + ": " + password + " (长度:" + password.length() + ")");
            
            if (password.length() < 8 || password.length() > 12) {
                System.out.println("  ❌ 长度错误！");
                System.exit(1);
            }
        }
        System.out.println("✅ 测试5通过");
        
        System.out.println("\n========================================");
        System.out.println("🎉 所有测试通过！");
        System.out.println("========================================");
    }
}
EOF

echo "3. 编译测试程序..."
javac -d /tmp \
    -cp "/root/jc-test/backend/target/test-classes:/root/jc-test/backend/target/classes" \
    /tmp/TestRuleDetector.java 2>&1

if [ $? -eq 0 ]; then
    echo "✅ 编译成功"
else
    echo "❌ 编译失败"
    exit 1
fi

echo ""
echo "4. 运行测试..."
echo "----------------------------------------"
java -cp "/tmp:/root/jc-test/backend/target/test-classes:/root/jc-test/backend/target/classes:$(find ~/.m2/repository/org/slf4j -name '*.jar' | tr '\n' ':')$(find ~/.m2/repository/ch/qos/logback -name '*.jar' | tr '\n' ':')" \
    TestRuleDetector

echo ""
echo "测试完成！"
