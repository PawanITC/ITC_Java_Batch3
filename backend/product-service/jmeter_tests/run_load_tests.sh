#!/bin/bash
# Load Testing Script for Product Service
# This script runs all JMeter load tests and generates reports

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
JMETER_HOME="/path/to/jmeter"  # Update this path
TEST_PLANS_DIR="jmeter_tests"
RESULTS_DIR="load_test_results"
BASE_URL="http://localhost:8080"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Create results directory
mkdir -p "$RESULTS_DIR"

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}Product Service Load Testing Script${NC}"
echo -e "${YELLOW}========================================${NC}"

# Function to run a test
run_test() {
    local test_name=$1
    local test_file=$2
    local threads=$3
    local rampup=$4
    local duration=$5

    echo -e "\n${YELLOW}Running: $test_name${NC}"
    echo "  Threads: $threads"
    echo "  Ramp-up: $rampup seconds"
    echo "  Duration: $duration seconds"

    local result_file="$RESULTS_DIR/${test_name}_${TIMESTAMP}.jtl"
    local log_file="$RESULTS_DIR/${test_name}_${TIMESTAMP}.log"
    local report_dir="$RESULTS_DIR/${test_name}_${TIMESTAMP}_report"

    # Run JMeter test
    "$JMETER_HOME/bin/jmeter" -n \
        -t "$TEST_PLANS_DIR/$test_file" \
        -l "$result_file" \
        -j "$log_file" \
        -Jthreads=$threads \
        -Jrampup=$rampup \
        -Jduration=$duration \
        -Jbase_url=$BASE_URL \
        -e -o "$report_dir"

    # Check results
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Test completed successfully${NC}"
        echo -e "${GREEN}  Results: $result_file${NC}"
        echo -e "${GREEN}  Report: $report_dir/index.html${NC}"
    else
        echo -e "${RED}✗ Test failed${NC}"
        return 1
    fi
}

# Test 1: Category Load Test (Light Load)
echo -e "\n${YELLOW}--- Test 1: Category Endpoints (Light Load) ---${NC}"
run_test "CategoryLoadTest" "CategoryLoadTest.jmx" 50 30 300

# Test 2: Product Load Test (Medium Load)
echo -e "\n${YELLOW}--- Test 2: Product Endpoints (Medium Load) ---${NC}"
run_test "ProductLoadTest" "ProductLoadTest.jmx" 100 60 600

# Test 3: Cart Load Test (Higher Load)
echo -e "\n${YELLOW}--- Test 3: Cart Workflow (Higher Load) ---${NC}"
run_test "CartLoadTest" "CartLoadTest.jmx" 200 120 600

# Test 4: Spike Test (Sudden Load)
echo -e "\n${YELLOW}--- Test 4: Spike Test (Sudden Load) ---${NC}"
run_test "SpikeTest" "CartLoadTest.jmx" 500 10 120

# Generate summary report
echo -e "\n${YELLOW}========================================${NC}"
echo -e "${YELLOW}Generating Summary Report${NC}"
echo -e "${YELLOW}========================================${NC}"

cat > "$RESULTS_DIR/LOAD_TEST_SUMMARY_${TIMESTAMP}.txt" << EOF
Load Test Summary Report
Generated: $(date)
Product Service URL: $BASE_URL

Test 1: Category Load Test
  - Threads: 50
  - Ramp-up: 30 seconds
  - Duration: 300 seconds (5 minutes)
  - Report: CategoryLoadTest_${TIMESTAMP}_report/index.html

Test 2: Product Load Test
  - Threads: 100
  - Ramp-up: 60 seconds
  - Duration: 600 seconds (10 minutes)
  - Report: ProductLoadTest_${TIMESTAMP}_report/index.html

Test 3: Cart Workflow Test
  - Threads: 200
  - Ramp-up: 120 seconds
  - Duration: 600 seconds (10 minutes)
  - Report: CartLoadTest_${TIMESTAMP}_report/index.html

Test 4: Spike Test
  - Threads: 500
  - Ramp-up: 10 seconds (sudden load)
  - Duration: 120 seconds (2 minutes)
  - Report: SpikeTest_${TIMESTAMP}_report/index.html

Expected Performance Targets:
- Average Response Time: < 200ms
- 95th Percentile: < 500ms
- Error Rate: < 1%
- Throughput: > 100 req/sec

Results Location: $(pwd)/$RESULTS_DIR/

Open HTML reports in browser to view detailed metrics and graphs.
EOF

echo -e "${GREEN}Summary report created: $RESULTS_DIR/LOAD_TEST_SUMMARY_${TIMESTAMP}.txt${NC}"

# Final summary
echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}All Load Tests Completed!${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "Results saved to: ${YELLOW}$(pwd)/$RESULTS_DIR/${NC}"
echo -e "View detailed reports at: ${YELLOW}$RESULTS_DIR/*/index.html${NC}"

