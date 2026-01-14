# ============================================================
# Test script for DATA1500 Øving 2 - Webapplikasjoner
# PowerShell version for Windows
#
# Usage: ./test-local.ps1
# ============================================================

$ErrorActionPreference = "Continue"

# Set UTF-8 encoding for proper Unicode support
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$TestDataDir = Join-Path $ScriptDir "testdata"

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  DATA1500 Øving 2 - Webapplikasjoner" -ForegroundColor Cyan
Write-Host "  Local Test Suite" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

$Passed = 0
$Failed = 0

function Run-Test {
    param(
        [string]$TestName,
        [scriptblock]$Command
    )

    Write-Host -NoNewline "  Testing: $TestName... "

    try {
        $result = & $Command

        if ($result) {
            Write-Host "✅" -ForegroundColor Green
            $script:Passed++
        } else {
            Write-Host "❌" -ForegroundColor Red
            $script:Failed++
        }
    } catch {
        Write-Host "❌" -ForegroundColor Red
        $script:Failed++
        return $false
    }
}
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
# ============================================================
# OPPGAVE 1: Enkel READ-API
# ============================================================

Write-Host ""
Write-Host "--- Oppgave 1: Enkel READ-API ---" -ForegroundColor Yellow

$Oppgave1Dir = Join-Path $ScriptDir "oppgave1"
if (Test-Path $Oppgave1Dir) {
    Push-Location $Oppgave1Dir

    Write-Host -NoNewline "Compiling StudentAPI.java... "
    javac StudentAPI.java 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅" -ForegroundColor Green

        # Start server in background
        $p1 = Start-Process -FilePath "java" -ArgumentList @("StudentAPI", "9001", "$TestDataDir/data/studenter.csv") -NoNewWindow -PassThru -RedirectStandardOutput ([IO.Path]::GetTempPath() + "server.log") -RedirectStandardError ([IO.Path]::GetTempPath() + "server.err")
        Start-Sleep -Seconds 1

        # Test 1: Get all students
        Run-Test "GET /api/students" {
            $response = curl.exe -s http://localhost:9001/api/students 2>$null
            $response | Select-String "Mickey" -Quiet
        }

        # Test 2: Get specific student
        Run-Test "GET /api/students/101" {
            $response = curl.exe -s http://localhost:9001/api/students/101 2>$null
            $response | Select-String '"id":101' -Quiet
        }

        # Test 3: Get student not found
        Run-Test "GET /api/students/999 (not found)" {
            $response = curl.exe -s http://localhost:9001/api/students/999 2>$null
            $response | Select-String "error" -Quiet
        }

        # Test 4: Health check
        Run-Test "GET /health" {
            $response = curl.exe -s http://localhost:9001/health 2>$null
            $response | Select-String "OK" -Quiet
        }

        # Kill server
        Stop-Process -Id $p1.Id -Force -ErrorAction SilentlyContinue
        Wait-Process -Id $p1.Id -ErrorAction SilentlyContinue

    } else {
        Write-Host "❌ Compilation failed" -ForegroundColor Red
        $Failed += 4
    }

    Pop-Location
} else {
    Write-Host "⚠️  Oppgave 1 directory not found" -ForegroundColor Yellow
}

# ============================================================
# OPPGAVE 2: API med Søk
# ============================================================

Write-Host ""
Write-Host "--- Oppgave 2: API med Søk og SQL Injection-illustrasjon ---" -ForegroundColor Yellow

$Oppgave2Dir = Join-Path $ScriptDir "oppgave2"
if (Test-Path $Oppgave2Dir) {
    Push-Location $Oppgave2Dir

    Write-Host -NoNewline "Compiling UserSearchAPI.java... "
    javac UserSearchAPI.java 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅" -ForegroundColor Green

        # Start server in background
        $p2 = Start-Process -FilePath "java" -ArgumentList @("UserSearchAPI", "9002", "$TestDataDir/data/brukere.csv") -NoNewWindow -PassThru -RedirectStandardOutput ([IO.Path]::GetTempPath() + "server.log") -RedirectStandardError ([IO.Path]::GetTempPath() + "server.err")
        Start-Sleep -Seconds 1

        # Test 1: Get all users
        Run-Test "GET /api/users" {
            $response = curl.exe -s http://localhost:9002/api/users 2>$null
            $response | Select-String "bruker1@epost.no" -Quiet
        }

        # Test 2: Search for user (vulnerable version)
        Run-Test "GET /api/search?email=bruker1@epost.no" {
            $response = curl.exe -s "http://localhost:9002/api/search?email=bruker1@epost.no" 2>$null
            $response | Select-String "bruker1@epost.no" -Quiet
        }

        # Test 3: Search for user (safe version)
        Run-Test "GET /api/search-safe?email=bruker5@epost.no" {
            $response = curl.exe -s "http://localhost:9002/api/search-safe?email=bruker5@epost.no" 2>$null
            $response | Select-String "bruker5@epost.no" -Quiet
        }

        # Test 4: Safe search with invalid email
        Run-Test "GET /api/search-safe with invalid email" {
            $response = curl.exe -s "http://localhost:9002/api/search-safe?email=invalid" 2>$null
            $response | Select-String "error" -Quiet
        }

        # Kill server
        Stop-Process -Id $p2.Id -Force -ErrorAction SilentlyContinue
        Wait-Process -Id $p2.Id -ErrorAction SilentlyContinue

    } else {
        Write-Host "❌ Compilation failed" -ForegroundColor Red
        $Failed += 4
    }

    Pop-Location
} else {
    Write-Host "⚠️  Oppgave 2 directory not found" -ForegroundColor Yellow
}

# ============================================================
# OPPGAVE 2 (NY): FileAccessAPI (Sikkerhet)
# ============================================================

Write-Host ""
Write-Host "--- Oppgave 2 (Ny): FileAccessAPI (Sikkerhet) ---" -ForegroundColor Yellow

$Oppgave2NYDir = Join-Path $ScriptDir "oppgave2"
if (Test-Path $Oppgave2NYDir) {
    Push-Location $Oppgave2NYDir

    Write-Host -NoNewline "Compiling FileAccessAPI.java... "
    javac FileAccessAPI.java 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅" -ForegroundColor Green

        # Start server in background
        $p5 = Start-Process -FilePath "java" -ArgumentList @("FileAccessAPI", "9005", "$TestDataDir/data") -NoNewWindow -PassThru -RedirectStandardOutput ([IO.Path]::GetTempPath() + "server.log") -RedirectStandardError ([IO.Path]::GetTempPath() + "server.err")
        Start-Sleep -Seconds 1

        # Test 1: Normal file reading (studenter.csv)
        Run-Test "GET /api/files?filename=studenter.csv" {
            $response = curl.exe -s "http://localhost:9005/api/files?filename=studenter.csv" 2>$null
            $response | Select-String "Mickey" -Quiet
        }

        # Test 2: Path Traversal (read secret.txt)
        Run-Test "GET /api/files?filename=../../secret.txt (Path Traversal)" {
            $response = curl.exe -s "http://localhost:9005/api/files?filename=../../secret.txt" 2>$null
            $response | Select-String "SuperHemmelig123" -Quiet
        }

        # Test 3: Normal search
        Run-Test "GET /api/search?query=bruker1" {
            $response = curl.exe -s "http://localhost:9005/api/search?query=bruker1" 2>$null
            $response | Select-String "bruker1" -Quiet
        }

        # Test 4: Simulated SQL Injection
        Run-Test "GET /api/search (SQL Injection)" {
            $response = curl.exe -s "http://localhost:9005/api/search?query=%27%20OR%20%271%27%3D%271" 2>$null
            $response | Select-String "bruker5" -Quiet
        }

        # Kill server
        Stop-Process -Id $p5.Id -Force -ErrorAction SilentlyContinue
        Wait-Process -Id $p5.Id -ErrorAction SilentlyContinue

    } else {
        Write-Host "❌ Compilation failed" -ForegroundColor Red
        $Failed += 4
    }

    Pop-Location
} else {
    Write-Host "⚠️  Oppgave 2 (Ny) directory not found" -ForegroundColor Yellow
}

# ============================================================
# OPPGAVE 3: CRUD-API med UPDATE
# ============================================================

Write-Host ""
Write-Host "--- Oppgave 3: CRUD-API med UPDATE ---" -ForegroundColor Yellow

$Oppgave3Dir = Join-Path $ScriptDir "oppgave3"
if (Test-Path $Oppgave3Dir) {
    Push-Location $Oppgave3Dir

    # Copy test data
    Copy-Item -Path "$TestDataDir/data/studenter.csv" -Destination "test_studenter.csv" -Force

    Write-Host -NoNewline "Compiling StudentCRUDAPI.java... "
    javac StudentCRUDAPI.java 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅" -ForegroundColor Green

        # Start server in background
        $p3 = Start-Process -FilePath "java" -ArgumentList @("StudentCRUDAPI", "9003", "test_studenter.csv") -NoNewWindow -PassThru -RedirectStandardOutput ([IO.Path]::GetTempPath() + "server.log") -RedirectStandardError ([IO.Path]::GetTempPath() + "server.err")
        Start-Sleep -Seconds 1

        # Test 1: GET student
        Run-Test "GET /api/students/101" {
            $response = curl.exe -s http://localhost:9003/api/students/101 2>$null
            $response | Select-String '"id":101' -Quiet
        }

        # Test 2: PUT (update) student
        Run-Test "PUT /api/students/101" {
            $response = curl.exe -s -X PUT -H "Content-Type: application/json" -d '{"name":"Mickey Mouse","program":"CS"}' http://localhost:9003/api/students/101 2>$null
            $response | Select-String "Mickey Mouse" -Quiet
        }

        # Test 3: GET all students
        Run-Test "GET /api/students" {
            $response = curl.exe -s http://localhost:9003/api/students 2>$null
            $response | Select-String "Mickey" -Quiet
        }

        # Test 4: POST (create) new student
        Run-Test "POST /api/students" {
            $response = curl.exe -s -X POST -H "Content-Type: application/json" -d '{"name":"NewStudent","program":"EE"}' http://localhost:9003/api/students 2>$null
            $response | Select-String "NewStudent" -Quiet
        }

        # Kill server
        Stop-Process -Id $p3.Id -Force -ErrorAction SilentlyContinue
        Wait-Process -Id $p3.Id -ErrorAction SilentlyContinue

    } else {
        Write-Host "❌ Compilation failed" -ForegroundColor Red
        $Failed += 4
    }

    # Cleanup
    Remove-Item -Path "test_studenter.csv" -Force -ErrorAction SilentlyContinue

    Pop-Location
} else {
    Write-Host "⚠️  Oppgave 3 directory not found" -ForegroundColor Yellow
}

# ============================================================
# OPPGAVE 4: CRUD-API med DELETE og Analytics
# ============================================================

Write-Host ""
Write-Host "--- Oppgave 4: CRUD-API med DELETE og Analytics ---" -ForegroundColor Yellow

$Oppgave4Dir = Join-Path $ScriptDir "oppgave4"
if (Test-Path $Oppgave4Dir) {
    Push-Location $Oppgave4Dir

    # Copy test data
    Copy-Item -Path "$TestDataDir/data/studenter.csv" -Destination "test_studenter.csv" -Force
    Copy-Item -Path "$TestDataDir/data/quiz-res.csv" -Destination "test_quiz_res.csv" -Force

    Write-Host -NoNewline "Compiling QuizAnalyticsAPI.java... "
    javac QuizAnalyticsAPI.java 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅" -ForegroundColor Green

        # Start server in background
        $p4 = Start-Process -FilePath "java" -ArgumentList @("QuizAnalyticsAPI", "9004", "test_studenter.csv", "test_quiz_res.csv") -NoNewWindow -PassThru -RedirectStandardOutput ([IO.Path]::GetTempPath() + "server.log") -RedirectStandardError ([IO.Path]::GetTempPath() + "server.err")
        Start-Sleep -Seconds 1

        # Test 1: GET students
        Run-Test "GET /api/students" {
            $response = curl.exe -s http://localhost:9004/api/students 2>$null
            $response | Select-String "Mickey" -Quiet
        }

        # Test 2: GET quiz stats
        Run-Test "GET /api/analytics/quiz-stats" {
            $response = curl.exe -s http://localhost:9004/api/analytics/quiz-stats 2>$null
            $response | Select-String "average_score" -Quiet
        }

        # Test 3: GET student stats
        Run-Test "GET /api/analytics/student-stats/101" {
            $response = curl.exe -s http://localhost:9004/api/analytics/student-stats/101 2>$null
            $response | Select-String "average_percentage" -Quiet
        }

        # Test 4: DELETE student
        Run-Test "DELETE /api/students/101" {
            curl.exe -s -X DELETE http://localhost:9004/api/students/101 2>$null | Out-Null
            Start-Sleep -Seconds 1
            $response = curl.exe -s http://localhost:9004/api/students/101 2>$null
            $response | Select-String "error" -Quiet
        }

        # Kill server
        Stop-Process -Id $p4.Id -Force -ErrorAction SilentlyContinue
        Wait-Process -Id $p4.Id -ErrorAction SilentlyContinue

    } else {
        Write-Host "❌ Compilation failed" -ForegroundColor Red
        $Failed += 4
    }

    # Cleanup
    Remove-Item -Path "test_studenter.csv" -Force -ErrorAction SilentlyContinue
    Remove-Item -Path "test_quiz_res.csv" -Force -ErrorAction SilentlyContinue

    Pop-Location
} else {
    Write-Host "⚠️  Oppgave 4 directory not found" -ForegroundColor Yellow
}

# ============================================================
# Summary
# ============================================================

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  Test Summary" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "Passed: $Passed" -ForegroundColor Green
Write-Host "Failed: $Failed" -ForegroundColor Red
Write-Host ""

Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue

if ($Failed -eq 0) {
    Write-Host "🎉 All tests passed! Your code is ready to submit." -ForegroundColor Green
    exit 0
} else {
    Write-Host "⚠️  Some tests failed. Please review your code." -ForegroundColor Red
    exit 1
}
