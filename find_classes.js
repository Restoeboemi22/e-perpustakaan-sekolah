const fs = require('fs');
const path = 'public/assets/index-xrVAwBp2-v3.js';

try {
  const content = fs.readFileSync(path, 'utf8');
  
  // 1. Verify new array exists
  const newArray = '["VII-A","VII-B","VII-C","VIII-A","VIII-B","VIII-C","IX-A","IX-B","IX-C"]';
  const indexNew = content.indexOf(newArray);
  
  if (indexNew !== -1) {
    console.log(`[OK] Found NEW array at index ${indexNew}`);
    console.log('Context:', content.substring(indexNew, indexNew + 100) + '...');
  } else {
    console.log('[FAIL] NEW array not found!');
  }

  // 2. Check for leftover old values
  const oldValues = ["7A","7B","7C","8A","8B","8C","9A","9B","9C"];
  let foundOld = false;
  
  console.log('\nChecking for leftovers...');
  oldValues.forEach(val => {
    // Search for "7A" (with quotes) to avoid matching part of other strings if possible, 
    // though in minified code it might be tricky.
    // We look for exact string matches including quotes.
    const searchVal = `"${val}"`;
    const idx = content.indexOf(searchVal);
    if (idx !== -1) {
      console.log(`[WARNING] Found old value ${searchVal} at index ${idx}`);
      console.log('Context:', content.substring(idx - 20, idx + 20));
      foundOld = true;
    }
  });
  
  if (!foundOld) {
    console.log('[OK] No old values ("7A", etc.) found.');
  }

} catch (err) {
  console.error(err);
}
