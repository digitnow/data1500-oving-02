#!/bin/bash

# ============================================================
# Test script for DATA1500 Øving 2 - Webapplikasjoner
# Kompatibel med: macOS og Linux
# 
# Bruk: ./test-local.sh
# ============================================================

# Farger for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Tellere
PASSED=0
FAILED=0

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Test data directory
TESTDATA_DIR="${SCRIPT_DIR}/testdata"

# Funksjon for å kjøre test
run_test() {
    local test_name=$1
    local command=$2
    
    echo -n "  Testing: $test_name... "
    
    if eval "$command" > /dev/null 2>&1; then
        echo -e "${GREEN}✅${NC}"
        ((PASSED++))
    else
        echo -e "${RED}❌${NC}"
        ((FAILED++))
    fi
}

echo "================================================"
echo "  DATA1500 Øving 2 - Webapplikasjoner"
echo "  Local Test Suite"
echo "================================================"
echo ""

# ============================================================
# OPPGAVE 1: Enkel READ-API
# ============================================================
echo ""
echo "--- Oppgave 1: Enkel READ-API ---"
if [ -d "oppgave1" ]; then
    cd oppgave1
    
    # Compile
    echo -n "Compiling StudentAPI.java... "
    if javac StudentAPI.java 2>/dev/null; then
        echo -e "${GREEN}✅${NC}"
        
        # Start server in background
        java StudentAPI 9001 ${TESTDATA_DIR}/data/studenter.csv > /dev/null 2>&1 &
        SERVER_PID=$!
        sleep 1
        
        # Test 1: Hent alle studenter
        run_test "GET /api/students" \
            "curl -s http://localhost:9001/api/students | grep -q 'Mickey'"
        
        # Test 2: Hent spesifikk student
        run_test "GET /api/students/101" \
            "curl -s http://localhost:9001/api/students/101 | grep -q '\"id\":101'"
        
        # Test 3: Hent student som ikke finnes
        run_test "GET /api/students/999 (not found)" \
            "curl -s http://localhost:9001/api/students/999 | grep -q 'error'"
        
        # Test 4: Health check
        run_test "GET /health" \
            "curl -s http://localhost:9001/health | grep -q 'OK'"
        
        # Kill server
        kill $SERVER_PID 2>/dev/null
        wait $SERVER_PID 2>/dev/null
        
    else
        echo -e "${RED}❌ Compilation failed${NC}"
        ((FAILED+=4))
    fi
    
    cd ..
else
    echo -e "${YELLOW}⚠️  Oppgave 1 directory not found${NC}"
fi

# ============================================================
# OPPGAVE 2: API med Søk
# ============================================================
echo ""
echo "--- Oppgave 2: API med Søk og SQL Injection-illustrasjon ---"
if [ -d "oppgave2" ]; then
    cd oppgave2
    
    # Compile
    echo -n "Compiling UserSearchAPI.java... "
    if javac UserSearchAPI.java 2>/dev/null; then
        echo -e "${GREEN}✅${NC}"
        
        # Start server in background
        java UserSearchAPI 9002 ${TESTDATA_DIR}/data/brukere.csv > /dev/null 2>&1 &
        SERVER_PID=$!
        sleep 1
        
        # Test 1: Hent alle brukere
        run_test "GET /api/users" \
            "curl -s http://localhost:9002/api/users | grep -q 'bruker1@epost.no'"
        
        # Test 2: Søk etter bruker (sårbar versjon)
        run_test "GET /api/search?email=bruker1@epost.no" \
            "curl -s 'http://localhost:9002/api/search?email=bruker1@epost.no' | grep -q 'bruker1@epost.no'"
        
        # Test 3: Søk etter bruker (sikker versjon)
        run_test "GET /api/search-safe?email=bruker5@epost.no" \
            "curl -s 'http://localhost:9002/api/search-safe?email=bruker5@epost.no' | grep -q 'bruker5@epost.no'"
        
        # Test 4: Sikker søk med ugyldig e-post
        run_test "GET /api/search-safe with invalid email" \
            "curl -s 'http://localhost:9002/api/search-safe?email=invalid' | grep -q 'error'"
        
        # Kill server
        kill $SERVER_PID 2>/dev/null
        wait $SERVER_PID 2>/dev/null
        
    else
        echo -e "${RED}❌ Compilation failed${NC}"
        ((FAILED+=4))
    fi
    
    cd ..
else
    echo -e "${YELLOW}⚠️  Oppgave 2 directory not found${NC}"
fi

# ============================================================
# OPPGAVE 2 (NY): FileAccessAPI (Sikkerhet)
# ============================================================
echo ""
echo "--- Oppgave 2 (Ny): FileAccessAPI (Sikkerhet) ---"
if [ -d "oppgave2" ]; then
    cd oppgave2
    
    # Compile
    echo -n "Compiling FileAccessAPI.java... "
    if javac FileAccessAPI.java 2>/dev/null; then
        echo -e "${GREEN}✅${NC}"
        
        # Start server in background
        # Note: We point to the 'testdata/data' directory relative to where we run the server
        java FileAccessAPI 9005 ${TESTDATA_DIR}/data > /dev/null 2>&1 &
        SERVER_PID=$!
        sleep 1
        
        # Test 1: Normal fil-lesing (studenter.csv)
        run_test "GET /api/files?filename=studenter.csv" \
            "curl -s 'http://localhost:9005/api/files?filename=studenter.csv' | grep -q 'Mickey'"
            
        # Test 2: Path Traversal (lese secret.txt som ligger to nivåer opp)
        # Vi antar at serveren kjører i oppgave2, data-mappen er ../testdata/data.
        # secret.txt ligger i roten av data1500-oving-02, dvs ../../secret.txt fra data-mappen.
        run_test "GET /api/files?filename=../../secret.txt (Path Traversal)" \
            "curl -s 'http://localhost:9005/api/files?filename=../../secret.txt' | grep -q 'SuperHemmelig123'"
            
        # Test 3: Normalt søk
        run_test "GET /api/search?query=bruker1" \
            "curl -s 'http://localhost:9005/api/search?query=bruker1' | grep -q 'bruker1'"
            
        # Test 4: Simulert SQL Injection
        # Vi må URL-encode ' OR '1'='1 -> %27%20OR%20%271%27%3D%271
        run_test "GET /api/search (SQL Injection)" \
            "curl -s \"http://localhost:9005/api/search?query=%27%20OR%20%271%27%3D%271\" | grep -q 'bruker5'"
            ' OR '1'='1
        
        # Kill server
        kill $SERVER_PID 2>/dev/null
        wait $SERVER_PID 2>/dev/null
        
    else
        echo -e "${RED}❌ Compilation failed${NC}"
        ((FAILED+=4))
    fi
    
    cd ..
else
    echo -e "${YELLOW}⚠️  Oppgave 2 (Ny) directory not found${NC}"
fi

# ============================================================
# OPPGAVE 3: CRUD-API med UPDATE
# ============================================================
echo ""
echo "--- Oppgave 3: CRUD-API med UPDATE ---"
if [ -d "oppgave3" ]; then
    cd oppgave3
    
    # Lag kopi av test-data
    cp ${TESTDATA_DIR}/data/studenter.csv test_studenter.csv
    
    # Compile
    echo -n "Compiling StudentCRUDAPI.java... "
    if javac StudentCRUDAPI.java 2>/dev/null; then
        echo -e "${GREEN}✅${NC}"
        
        # Start server in background
        java StudentCRUDAPI 9003 test_studenter.csv > /dev/null 2>&1 &
        SERVER_PID=$!
        sleep 1
        
        # Test 1: GET student
        run_test "GET /api/students/101" \
            "curl -s http://localhost:9003/api/students/101 | grep -q '\"id\":101'"
        
        # Test 2: PUT (update) student
        run_test "PUT /api/students/101" \
            "curl -s -X PUT -H 'Content-Type: application/json' -d '{\"name\":\"Mickey Mouse\",\"program\":\"CS\"}' http://localhost:9003/api/students/101 | grep -q 'Mickey Mouse'"
        
        # Test 3: GET all students
        run_test "GET /api/students" \
            "curl -s http://localhost:9003/api/students | grep -q 'Mickey'"
        
        # Test 4: POST (create) new student
        run_test "POST /api/students" \
            "curl -s -X POST -H 'Content-Type: application/json' -d '{\"name\":\"NewStudent\",\"program\":\"EE\"}' http://localhost:9003/api/students | grep -q 'NewStudent'"
        
        # Kill server
        kill $SERVER_PID 2>/dev/null
        wait $SERVER_PID 2>/dev/null
        
    else
        echo -e "${RED}❌ Compilation failed${NC}"
        ((FAILED+=4))
    fi
    
    # Cleanup
    rm -f test_studenter.csv
    
    cd ..
else
    echo -e "${YELLOW}⚠️  Oppgave 3 directory not found${NC}"
fi

# ============================================================
# OPPGAVE 4: CRUD-API med DELETE og Analytics
# ============================================================
echo ""
echo "--- Oppgave 4: CRUD-API med DELETE og Analytics ---"
if [ -d "oppgave4" ]; then
    cd oppgave4
    
    # Lag kopier av test-data
    cp ${TESTDATA_DIR}/data/studenter.csv test_studenter.csv
    cp ${TESTDATA_DIR}/data/quiz-res.csv test_quiz_res.csv
    
    # Compile
    echo -n "Compiling QuizAnalyticsAPI.java... "
    if javac QuizAnalyticsAPI.java 2>/dev/null; then
        echo -e "${GREEN}✅${NC}"
        
        # Start server in background
        java QuizAnalyticsAPI 9004 test_studenter.csv test_quiz_res.csv > /dev/null 2>&1 &
        SERVER_PID=$!
        sleep 1
        
        # Test 1: GET students
        run_test "GET /api/students" \
            "curl -s http://localhost:9004/api/students | grep -q 'Mickey'"
        
        # Test 2: GET quiz stats
        run_test "GET /api/analytics/quiz-stats" \
            "curl -s http://localhost:9004/api/analytics/quiz-stats | grep -q 'average_score'"
        
        # Test 3: GET student stats
        run_test "GET /api/analytics/student-stats/101" \
            "curl -s http://localhost:9004/api/analytics/student-stats/101 | grep -q 'average_percentage'"
        
        # Test 4: DELETE student
        run_test "DELETE /api/students/101" \
            "curl -s -X DELETE http://localhost:9004/api/students/101 && sleep 1 && curl -s http://localhost:9004/api/students/101 | grep -q 'error'"
        
        # Kill server
        kill $SERVER_PID 2>/dev/null
        wait $SERVER_PID 2>/dev/null
        
    else
        echo -e "${RED}❌ Compilation failed${NC}"
        ((FAILED+=4))
    fi
    
    # Cleanup
    rm -f test_studenter.csv test_quiz_res.csv
    
    cd ..
else
    echo -e "${YELLOW}⚠️  Oppgave 4 directory not found${NC}"
fi

# ============================================================
# Summary
# ============================================================
echo ""
echo "================================================"
echo "  Test Summary"
echo "================================================"
echo -e "Passed: ${GREEN}${PASSED}${NC}"
echo -e "Failed: ${RED}${FAILED}${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 All tests passed! Your code is ready to submit.${NC}"
    exit 0
else
    echo -e "${RED}⚠️  Some tests failed. Please review your code.${NC}"
    exit 1
fi
