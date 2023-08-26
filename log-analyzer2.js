 
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
  //const pattern = /Benchmark (\d+)[^]*?-t (\d+)[^]*?-s (\d+)[^]*?-j (\d+)[^]*?Task times: LongSummaryStatistics\{(.+)\}[^§]*?Total time:.*?(\d+\.\d+)s/g;
  const pattern1 = /(\d+):\s([^]*?-t (\d+)[^]*?-s (\d+)[^]*?-j (\d+))/g;
  const pattern2 = /Task times: LongSummaryStatistics\{(.+)\}[^§]*?Total time:.*?(\d+\.\d+)s/g
  const fdPattern = /Number of dependencies:	(\d+)/g

  data = data.split("Benchmark")
  result = data.map(str => {

    // Organize the extracted information and format the output
    let meta = [...str.matchAll(pattern1)].map(match => {
        let [, i, command, t, s, j] = match

        i = parseInt(i);
        t = parseInt(t);
        s = parseInt(s);
        j = parseInt(j);

        return {
        i,
        command,
        t, s, j,
        };
    });
    if(meta.length > 1)
        console.error("ERR! Multiple benchmarks in single benchmark", meta)
    if(!meta[0]) {
      console.error("ERR! No meta info found");
      process.exit(1)
    }
    meta = meta[0]

    let fdcount = [...str.matchAll(fdPattern)].map(match => {
      let [, fds] = match
      return fds;
    })[0];

    meta.fds = fdcount;

    let raw = [...str.matchAll(pattern2)].map(match => {
        let [, stats, time] = match

        time = parseFloat(time);

        return {
            ...parseStatisticsString(stats),
            time
        };
    });
    return {
        meta,
        raw,
        mean: applytoall(zip(raw), mean),
        median: applytoall(zip(raw), median),
        stdev: applytoall(zip(raw), stdev),
    }
  })

  // Print the formatted output using JSON
  const output_json = JSON.stringify(result, null, 4);
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

function zip(arr) {
    return arr.reduce((result, obj) => {
        for (const key in obj) {
          if (!result[key]) {
            result[key] = [];
          }
          result[key].push(obj[key]);
        }
        return result;
      }, {});
}

function mean(arr) {
    if (arr.length === 0) {
      return 0; // Handle empty array case
    }
    
    const sum = arr.reduce((acc, val) => acc + val, 0);
    return sum / arr.length;
  }
  
  function median(arr) {
    if (arr.length === 0) {
      return undefined; // Handle empty array case
    }
  
    const sortedArr = arr.slice().sort((a, b) => a - b);
    const middleIndex = Math.floor(sortedArr.length / 2);
  
    if (sortedArr.length % 2 === 0) {
      return (sortedArr[middleIndex - 1] + sortedArr[middleIndex]) / 2;
    } else {
      return sortedArr[middleIndex];
    }
  }
  
  function stdev(arr) {
    if (arr.length === 0) {
      return undefined; // Handle empty array case
    }
    
    const meanValue = mean(arr);
    const squaredDifferences = arr.map(val => Math.pow(val - meanValue, 2));
    const variance = mean(squaredDifferences);
    return Math.sqrt(variance);
  }

  function applytoall(obj, method) {
    const result = {};
    for (const key of Object.keys(obj)) {
        result[key] = method(obj[key]);
    }
    return result;
  }