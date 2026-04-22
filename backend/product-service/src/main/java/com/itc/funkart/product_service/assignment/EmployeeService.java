package com.itc.funkart.product_service.assignment;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EmployeeService {
    public Map<String, Employee> getHighestSalary(List<Employee> employees){
        return   employees.stream()
                .collect(Collectors.groupingBy(
                        Employee::getDepartment,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparing(Employee::getSalary)),
                                Optional::get
                        )
                ));

    }
}

//public Map<String, Double> getHighestSalaryOnly(List<Employee> employees) {
//    return employees.stream()
//            .collect(Collectors.groupingBy(
//                    Employee::getDepartment,
//                    Collectors.collectingAndThen(
//                            Collectors.maxBy(Comparator.comparing(Employee::getSalary)),
//                            emp -> emp.map(Employee::getSalary).orElse(0.0)
//                    )
//            ));
//}
