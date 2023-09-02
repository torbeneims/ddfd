 
const readline = require('readline');
const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
  terminal: false
});

let data = '';

// Read data from stdin
rl.on('line', line => {
  data += line + '\n';
}).on('close', () => {
  // Define the regex pattern
  const pattern = /Benchmark (\d+)[^]*?-t (\d+)[^]*?-s (\d+)[^]*?-j (\d+)[^]*?Task times: LongSummaryStatistics\{(.+)\}[^§]*?Total time:.*?(\d+\.\d+)s/g;

  // Find all matches using the regex pattern
  const matches = [...data.matchAll(pattern)];

  // Organize the extracted information and format the output
  const results = [];
  for (const match of matches) {
    let [,i, t, s, j, times, total] = match


    times = parseStatisticsString(times);
    i = parseInt(i);
    t = parseInt(t);
    s = parseInt(s);
    j = parseInt(j);
    total = parseFloat(total);

    const result = {
      i,
      t, s, j,
      ...times,
      total
    };
    results.push(result);
  }

  // Print the formatted output using JSON
  const output_json = JSON.stringify(results, null, 4);
  console.log(output_json);
});

function parseStatisticsString(input) {
  const pattern = /count=(\d+),\s+sum=(\d+),\s+min=(\d+),\s+average=(\d+\.\d+),\s+max=(\d+)/;
  const match = input.match(pattern);

  if (match) {
    return {
      count: parseInt(match[1]),
      sum: parseInt(match[2]),
      min: parseInt(match[3]),
      average: parseFloat(match[4]),
      max: parseInt(match[5])
    };
  } else {
    return null; // Return null for invalid input
  }
}
