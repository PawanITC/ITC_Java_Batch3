import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

public class HighestSalaryByDepartment {

    static class Employee {
        String name;
        String department;
        double salary;

        Employee(String name, String department, double salary) {
            this.name = name;
            this.department = department;
            this.salary = salary;
        }
    }

    public static void main(String[] args) {
        List<Employee> employees = List.of(
                new Employee("Abbas", "IT", 60000),
                new Employee("Anjali", "IT", 72000),
                new Employee("Sunil", "HR", 65000),
                new Employee("Dipendra", "HR", 64000),
                new Employee("Abu", "IT", 68000),
                new Employee("Priyanthan", "Sales", 70000)
        );

        Map<String, Optional<Employee>> highestSalaryByDept =
                employees.stream()
                        .collect(Collectors.groupingBy(
                                e -> e.department,
                                Collectors.maxBy(Comparator.comparingDouble(e -> e.salary))
                        ));

        highestSalaryByDept.forEach((dept, emp) ->
                emp.ifPresent(e ->
                        System.out.println(dept + " -> " + e.name + " -> " + e.salary)
                )
        );
    }
}