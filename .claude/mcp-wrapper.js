const { spawn } = require('child_process');
const path = require('path');
const readline = require('readline');

const projectRoot = path.join(__dirname, '..');
const isWindows = process.platform === 'win32';
const gradleBin = isWindows ? 'gradlew.bat' : './gradlew';
const gradlew = path.join(projectRoot, gradleBin);
const args = [
  '--no-daemon',
  '--console=plain',
  '--quiet',
  ':desktopApp:hotMcpServer'
];

const child = isWindows
  ? spawn(`"${gradlew}" ${args.join(' ')}`, { cwd: projectRoot, shell: true })
  : spawn(gradlew, args, { cwd: projectRoot });

process.stdin.pipe(child.stdin);
child.stderr.pipe(process.stderr);

const rl = readline.createInterface({
  input: child.stdout,
  terminal: false
});

rl.on('line', (line) => {
  const trimmed = line.trim();
  if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
    try {
      JSON.parse(trimmed);
      process.stdout.write(line + '\n');
    } catch (e) {
      process.stderr.write(`[Wrapper Filtered Out] ${line}\n`);
    }
  } else {
    process.stderr.write(`[Wrapper Filtered Out] ${line}\n`);
  }
});

child.on('close', (code) => {
  process.exit(code);
});

child.on('error', (err) => {
  process.exit(1);
});
