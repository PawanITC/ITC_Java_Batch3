package com.itc.funkart.product.assignment;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EmployeeService {
    public Map<String, Employee> getHighestSalary(List<Employee> employees) {
        return employees.stream()
                .collect(Collectors.toMap(
                        Employee::getDepartment,
                        Function.identity(),
                        BinaryOperator.maxBy(Comparator.comparing(Employee::getSalary))));

    }
}

