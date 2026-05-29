package org.dpbe.old.runner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * 콘솔 입출력 헬퍼 (유스케이스 외부의 구동 코드)
 *
 * Scanner 기반의 입력 받기, 메뉴 출력, 검증 등을 제공한다.
 * 도메인 로직과 무관한 UI 보조 클래스이다.
 */
public class ConsoleHelper {

    private static final Scanner SCANNER = new Scanner(System.in);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ConsoleHelper() {}

    /** 한 줄 입력받기 (빈 입력 허용) */
    public static String readLine(String prompt) {
        System.out.print(prompt);
        try {
            if (!SCANNER.hasNextLine()) {
                System.out.println("\n[시스템] 입력 종료. 프로그램을 종료합니다.");
                System.exit(0);
            }
            return SCANNER.nextLine().trim();
        } catch (Exception e) {
            System.out.println("\n[시스템] 입력 오류. 프로그램을 종료합니다.");
            System.exit(0);
            return ""; // unreachable
        }
    }

    /** 비어있지 않은 문자열 입력받기 (빈 입력 시 재입력 요구) */
    public static String readNonEmpty(String prompt) {
        while (true) {
            String input = readLine(prompt);
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("  ⚠️  값을 입력해주세요.");
        }
    }

    /** 정수 입력받기 (유효하지 않으면 재입력 요구) */
    public static int readInt(String prompt) {
        while (true) {
            String input = readLine(prompt);
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("  ⚠️  정수를 입력해주세요.");
            }
        }
    }

    /** 양의 정수 입력받기 */
    public static int readPositiveInt(String prompt) {
        while (true) {
            int v = readInt(prompt);
            if (v > 0) return v;
            System.out.println("  ⚠️  0보다 큰 값을 입력해주세요.");
        }
    }

    /** 범위 내 정수 입력받기 */
    public static int readRangeInt(String prompt, int min, int max) {
        while (true) {
            int v = readInt(prompt);
            if (v >= min && v <= max) return v;
            System.out.println("  ⚠️  " + min + "~" + max + " 사이의 값을 입력해주세요.");
        }
    }

    /** long 입력받기 */
    public static long readLong(String prompt) {
        while (true) {
            String input = readLine(prompt);
            try {
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                System.out.println("  ⚠️  숫자를 입력해주세요.");
            }
        }
    }

    /** double 입력받기 */
    public static double readDouble(String prompt) {
        while (true) {
            String input = readLine(prompt);
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("  ⚠️  숫자를 입력해주세요.");
            }
        }
    }

    /** y/n 입력받기 */
    public static boolean readYesNo(String prompt) {
        while (true) {
            String input = readLine(prompt + " (y/n): ").toLowerCase();
            if (input.equals("y") || input.equals("yes")) return true;
            if (input.equals("n") || input.equals("no")) return false;
            System.out.println("  ⚠️  y 또는 n을 입력해주세요.");
        }
    }

    /** 날짜 입력받기 (yyyy-MM-dd) */
    public static LocalDate readDate(String prompt) {
        while (true) {
            String input = readLine(prompt + " (yyyy-MM-dd): ");
            try {
                return LocalDate.parse(input, DATE_FMT);
            } catch (DateTimeParseException e) {
                System.out.println("  ⚠️  yyyy-MM-dd 형식으로 입력해주세요.");
            }
        }
    }

    /** 일시 입력받기 (yyyy-MM-dd HH:mm) */
    public static LocalDateTime readDateTime(String prompt) {
        while (true) {
            String input = readLine(prompt + " (yyyy-MM-dd HH:mm): ");
            try {
                return LocalDateTime.parse(input, DATETIME_FMT);
            } catch (DateTimeParseException e) {
                System.out.println("  ⚠️  yyyy-MM-dd HH:mm 형식으로 입력해주세요.");
            }
        }
    }

    /** 옵션 메뉴 출력 후 1~size 사이 숫자 입력받기 */
    public static int readMenuChoice(String title, String... options) {
        System.out.println();
        if (title != null && !title.isEmpty()) {
            System.out.println(title);
        }
        for (int i = 0; i < options.length; i++) {
            System.out.println("  " + (i + 1) + ". " + options[i]);
        }
        while (true) {
            int v = readInt("  > 선택: ");
            if (v >= 1 && v <= options.length) return v;
            System.out.println("  ⚠️  1~" + options.length + " 사이의 숫자를 입력해주세요.");
        }
    }

    /** 다중 선택 (쉼표 구분) */
    public static List<Integer> readMultiChoice(String title, String... options) {
        System.out.println();
        if (title != null && !title.isEmpty()) {
            System.out.println(title);
        }
        for (int i = 0; i < options.length; i++) {
            System.out.println("  " + (i + 1) + ". " + options[i]);
        }
        while (true) {
            String input = readLine("  > 선택 (쉼표 구분, 예: 1,3): ");
            try {
                List<Integer> result = Arrays.stream(input.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                if (result.isEmpty()) {
                    System.out.println("  ⚠️  하나 이상 선택해주세요.");
                    continue;
                }
                boolean valid = result.stream().allMatch(i -> i >= 1 && i <= options.length);
                if (!valid) {
                    System.out.println("  ⚠️  유효한 번호를 입력해주세요.");
                    continue;
                }
                return result;
            } catch (NumberFormatException e) {
                System.out.println("  ⚠️  숫자만 입력해주세요.");
            }
        }
    }

    /** 구분선 출력 */
    public static void printDivider() {
        System.out.println("------------------------------------------");
    }

    public static void printDoubleDivider() {
        System.out.println("==========================================");
    }

    /** 진행 안내 (행위자/단계 명시) */
    public static void printStage(String actor, String message) {
        System.out.println("\n[" + actor + "] " + message);
    }

    /** 정보 출력 */
    public static void printInfo(String message) {
        System.out.println("  " + message);
    }

    /** 성공 출력 */
    public static void printSuccess(String message) {
        System.out.println("  ✓ " + message);
    }

    /** 경고/예외 출력 */
    public static void printWarning(String message) {
        System.out.println("  ⚠️  " + message);
    }

    /** 에러 출력 */
    public static void printError(String message) {
        System.out.println("  ❌ " + message);
    }

    /** Enter 대기 */
    public static void waitEnter() {
        readLine("\n  (Enter를 눌러 계속...)");
    }
}
