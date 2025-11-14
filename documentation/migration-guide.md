# Migration Guide - Old to New Structure

## Overview

This guide helps you migrate from the old phase-based structure to the new feature-based documentation and testing structure.

---

## What Changed?

### Documentation Structure

**Old (Phase-based)**:
```
/
├── README.md (very long)
├── PHASE3_TESTING.md
└── CROSS_BROWSER_TESTING.md
```

**New (Feature-based)**:
```
/
├── README.md (short intro)
└── documentation/
    ├── testing-guide.md
    ├── websocket-testing.md
    ├── concurrency-testing.md
    ├── cross-browser-testing.md
    └── history-testing.md
```

---

### Test Scripts

**Old**:
- `run_phase3_tests.sh` - All tests in one script
- `test_websocket.sh` - WebSocket tests

**New**:
- `run_all_tests.sh` - Master test runner
- `test_websocket_reconnect.sh` - Specific WebSocket reconnection test
- `test_concurrency.sh` - Concurrency test (unchanged)
- `smoke_test.sh` - Smoke test (unchanged)

---

## Migration Steps

### Step 1: Clean Up Old Files

Run the cleanup script:
```bash
chmod +x cleanup_old_files.sh
./cleanup_old_files.sh
```

This removes:
- `documentation/PHASE3_TESTING.md`
- `documentation/CROSS_BROWSER_TESTING.md`
- `run_phase3_tests.sh`
- `test_websocket.sh`

---

### Step 2: Update Your Workflow

**Old command**:
```bash
./run_phase3_tests.sh
```

**New command**:
```bash
./run_all_tests.sh
```

---

### Step 3: Update Bookmarks/References

If you have bookmarked or referenced old documentation:

| Old Reference | New Reference |
|--------------|---------------|
| `PHASE3_TESTING.md` | `documentation/testing-guide.md` |
| `CROSS_BROWSER_TESTING.md` | `documentation/cross-browser-testing.md` |
| Phase 1/2/3 concepts | Feature-based documentation |

---

### Step 4: Review New Documentation Structure

1. Start with `README.md` for project overview
2. Go to `documentation/testing-guide.md` for testing overview
3. Refer to specific guides as needed:
   - WebSocket issues → `websocket-testing.md`
   - Concurrency issues → `concurrency-testing.md`
   - Browser compatibility → `cross-browser-testing.md`
   - History feature → `history-testing.md`

---

## Key Changes

### No More "Phase" Terminology

**Before**: Phase 1, Phase 2, Phase 3
**After**: Features and functionality (WebSocket, Concurrency, History, etc.)

**Why**: More intuitive and feature-focused approach

---

### Reorganized Documentation

**Before**: Mixed content in README and scattered MD files
**After**: Clean separation:
- README = Quick start
- documentation/ = Detailed guides

---

### Simplified Test Scripts

**Before**: Monolithic test script
**After**: Individual test scripts with master runner

**Benefits**:
- Run specific tests independently
- Easier to add new tests
- Better separation of concerns

---

## Command Reference

### Old Commands → New Commands

| Old | New | Notes |
|-----|-----|-------|
| `./run_phase3_tests.sh` | `./run_all_tests.sh` | Runs all automated tests |
| `cat PHASE3_TESTING.md` | `cat documentation/testing-guide.md` | Testing overview |
| `cat CROSS_BROWSER_TESTING.md` | `cat documentation/cross-browser-testing.md` | Browser testing guide |
| Individual phase tests | Individual feature tests | Same test scripts, different names |

---

## FAQ

### Q: Will old scripts still work?

**A**: Old scripts (`run_phase3_tests.sh`, `test_websocket.sh`) will continue to work until you run the cleanup script. However, we recommend migrating to the new structure.

---

### Q: Do I need to update my CI/CD?

**A**: Yes, if you reference `run_phase3_tests.sh` in CI/CD:

**Old**:
```yaml
- run: ./run_phase3_tests.sh
```

**New**:
```yaml
- run: ./run_all_tests.sh
```

---

### Q: What if I have documentation links in other projects?

**A**: Update links to the new paths:
- `PHASE3_TESTING.md` → `documentation/testing-guide.md`
- `CROSS_BROWSER_TESTING.md` → `documentation/cross-browser-testing.md`

---

### Q: Can I keep both old and new structures temporarily?

**A**: Yes, the cleanup script is optional. You can keep old files during transition, but they won't be maintained.

---

## Troubleshooting Migration Issues

### Issue: Can't find documentation

**Solution**: Check `documentation/` folder:
```bash
ls documentation/
```

Expected files:
- testing-guide.md
- websocket-testing.md
- concurrency-testing.md
- cross-browser-testing.md
- history-testing.md

---

### Issue: Test scripts not found

**Solution**: Ensure you're using new script names:
```bash
ls -la *.sh
```

Look for:
- run_all_tests.sh
- test_concurrency.sh
- test_websocket_reconnect.sh
- smoke_test.sh

---

### Issue: Old scripts still present

**Solution**: Run cleanup:
```bash
./cleanup_old_files.sh
```

---

## Benefits of New Structure

1. **Cleaner README**: Short, focused introduction
2. **Better Organization**: Feature-based docs instead of phase-based
3. **Easier Navigation**: Clear documentation hierarchy
4. **Modular Tests**: Run individual tests independently
5. **Future-Proof**: Easy to add new features/tests
6. **No Jargon**: No "Phase 1/2/3" terminology

---

## Rollback (If Needed)

If you need to temporarily rollback:

1. **Don't run** `cleanup_old_files.sh`
2. **Keep using** old script names
3. **Reference** old documentation files

However, old structure won't receive updates or fixes.

---

## Getting Help

- Check `documentation/testing-guide.md` for comprehensive guide
- Review individual feature documentation as needed
- Open an issue if migration causes problems

---

## Summary

**Quick migration**:
```bash
# 1. Clean up old files
./cleanup_old_files.sh

# 2. Use new test runner
./run_all_tests.sh

# 3. Read new docs
cat documentation/testing-guide.md
```

That's it! You're migrated to the new structure.
