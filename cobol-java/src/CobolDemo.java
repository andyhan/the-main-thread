import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import cobol.calcinterest_h;

public class CobolDemo {

    public static void main(String[] args) throws Throwable {

        try (Arena arena = Arena.ofConfined()) {

            // Initialise the GnuCOBOL runtime before any COBOL module executes
            calcinterest_h.cob_init(0, MemorySegment.NULL);

            // Pass values as fixed-width decimal strings matching the PIC clauses:
            // PIC 9(7)V99 → 9 digits, decimal implied at position 7
            // PIC 9(3)V9999 → 7 digits, decimal implied at position 3
            // PIC 9(2) → 2 digits
            // PIC 9(9)V99 → 11 digits, decimal implied at position 9
            MemorySegment principal = arena.allocateFrom("001000000"); // 10000.00
            MemorySegment rate = arena.allocateFrom("0045000"); // 4.5000
            MemorySegment years = arena.allocateFrom("10");
            MemorySegment result = arena.allocate(12); // 11 digits + null

            calcinterest_h.CALCINTEREST(principal, rate, years, result);

            // Insert the implied decimal: PIC 9(9)V99 = 9 integer + 2 decimal digits
            String raw = result.getString(0);
            double output = Double.parseDouble(raw.substring(0, 9) + "." + raw.substring(9));
            System.out.printf("Compound interest result: %.2f%n", output);
        }
    }
}