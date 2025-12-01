# Updated Documentation for Cache Simulator and Load/Store Buffer

## Overview

This document describes the design, implementation, and behavior of the Cache Simulator and Load/Store Buffer (LSB) components used in the Tomasulo-based architecture project. It reflects the latest corrections and bug fixes.

---

# 1. System Overview

The system models a simplified memory hierarchy, including:

* **Direct-mapped write-back cache**
* **Main memory model** with byte-addressable storage
* **Load/Store Buffer** (LSB) ensuring memory ordering and address-dependence correctness
* **Support for word (4B) and doubleword (8B) load/store instructions**

The implementation focuses on correctness, clarity, and accurate hit/miss behavior.

---

# 2. Memory Organization

### 2.1 Addressing Model

* Memory is **byte-addressable**.
* Words are **4 bytes**, doublewords are **8 bytes**.
* All multi-byte operations assume **big-endian** ordering:

  * Most significant byte stored at the lowest address.

### 2.2 Memory Storage

Memory is implemented as a:

```java
byte[] memory = new byte[MEMORY_SIZE];
```

Reads/writes convert between bytes and 32/64-bit values.

### 2.3 Multi-byte Access Rules

* `readWord(addr)` reads bytes `[addr .. addr+3]`
* `readDoubleWord(addr)` reads:

  * word at `addr`
  * word at `addr+4`
* Doubleword operations **may cross cache block boundaries**, potentially causing:

  * Two separate cache accesses
  * Two separate misses

This behavior must be documented for test cases.

---

# 3. Cache Organization

### 3.1 Structure

The cache is a **direct-mapped** array of `CacheBlock` objects:

* `numLines = cacheSize / blockSize`
* Each line contains:

  * `tag`
  * `valid` flag
  * `dirty` flag
  * `data[]` of size `blockSize`

### 3.2 Address Breakdown

Given an address `A`:

* `blockNum = A / blockSize`
* `index = blockNum % numLines`
* `tag = blockNum / numLines`
* `blockStart = blockNum * blockSize`

### 3.3 CacheBlock Behavior

`CacheBlock` now stores **only per-line data**. It no longer computes whether an address belongs to its block.

### 3.4 Containment Logic Moved to CacheSimulator

Because `CacheBlock` does not know its `index`, the correct containment computation is now:

```java
blockNum = tag * numLines + index
blockStart = blockNum * blockSize
address ∈ [blockStart, blockStart + blockSize - 1]
```

Implemented inside:

```java
private boolean blockContainsAddress(int address, int index, int tag)
```

### 3.5 Replacement Policy

Direct-mapped: new block **overwrites existing one**.
If evicted block is dirty → write-back to memory.

### 3.6 Write Policy

* **Write-back** cache
* **Write-allocate**: store miss brings block into cache

---

# 4. Cache Hit/Miss Determination

Cache hit/miss is determined **at issue time**:

* When an L/S instruction is issued to the LSB, the cache is checked.
* The resulting latency is locked in.

### Important Limitation

Because miss/hit is computed **before** the instruction executes:

> Cache state may change before execution, causing inconsistencies with real hardware.

This simplification is acceptable for this project but must be noted.

---

# 5. Load/Store Buffer (LSB)

The LSB enforces **program-ordered memory consistency** and **address-dependence constraints**.

### 5.1 LSB Entry Fields

Each entry contains:

* `id` (monotonic increasing sequence number)
* `address` (if resolved)
* `value`
* `op` type (LD, SD, etc.)
* `busy`, `ready`, `executing`

### 5.2 Address Clash Logic (Corrected)

Old implementation assumed entries were stored in sorted order and used `break` — unsafe.

### Updated algorithm:

```java
for (Entry earlier : buffer) {
    if (earlier.id >= current.id) continue;
    if (!earlier.busy) continue;
    if (addressesOverlap(...)) return true;
}
return false;
```

This ensures correct ordering **regardless of internal list order**.

---

# 6. Instruction Behavior

### 6.1 Loads

* `L.W` loads 4 bytes
* `L.D` loads 8 bytes (two 4-byte reads)
* May cross block boundaries

### 6.2 Stores

* Store updates the cache if the block is resident
* Otherwise, block is fetched into cache (write-allocate)
* Dirty bit set on the cache block

---

# 7. Example: Block Boundary Doubleword

For block size 16 bytes:

* Block ranges: `[0–15], [16–31], ..., [96–111], [112–127]`

`LD at address 108` accesses bytes:

* `108–111` (block 96–111)
* `112–115` (block 112–127)

Thus:

* Hit on first half (after earlier load)
* Miss on second half (new block required)

---

# 8. Testing Recommendations

### Include these:

* Doubleword load crossing block boundary
* Two LSB entries with overlapping addresses
* Store followed by load to same address (must stall)
* Store causing eviction of a dirty block
* Load hitting on recently stored data (cache must provide value)

---

# 9. Limitations

* No in-flight cache block reservations
* Hit/miss determined at issue time
* No multi-cycle memory bus contention modeling
* Doubleword operations perform two independent word reads

---

# 10. Changelog (Fixes Applied)

* Removed `CacheBlock.containsAddress()` — replaced with correct logic in `CacheSimulator`.
* Fixed LSB ordering bug by enforcing explicit `earlier.id < current.id` comparison.
* Updated documentation to reflect doubleword boundary behavior.
* Clarified endianness behavior (big-endian).

---

# End of Documentation
