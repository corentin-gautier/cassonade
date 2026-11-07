// The client game tests in VS Code's Testing view. They are Fabric entrypoints, not JUnit tests, so the
// Java extension does not see them: this lists the classes registered in the test mod's fabric.mod.json
// and runs them with `./gradlew runClientGameTest -Ptests=…`.
const vscode = require('vscode');
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');

const MOD_JSON = 'src/gametest/resources/fabric.mod.json';
const SOURCES = 'src/gametest/java';
const SUFFIX = 'ClientGameTest';
// These need a flag of their own (see build.gradle), which selects them alone: each runs apart, and only
// when asked for by itself, as the site's screenshots and icons overwrite files in promo/site
const FLAGS = {
	SiteScreenshots: '-PsiteScreenshots',
	SiteIcons: '-PsiteIcons',
	Profile: '-Pprofile',
	ExploredWorld: '-PexploredWorld',
};
const FAILURE = 'Client gametests failed with an exception';
// Where the game's own takeScreenshot saves, in 1920×1080
const SCREENSHOTS = 'build/run/clientGameTest/screenshots';
const DEBUG_PORT = /Listening for transport dt_socket at address: (\d+)/;

function activate(context) {
	const folder = (vscode.workspace.workspaceFolders || []).find(f => fs.existsSync(path.join(f.uri.fsPath, MOD_JSON)));
	if (!folder) {
		return;
	}
	const root = folder.uri.fsPath;
	const controller = vscode.tests.createTestController('cassonadeClientTests', 'Cassonade client tests');
	context.subscriptions.push(controller);
	// Fabric runs the tests in the order of the entrypoints: a run's results are read in that order
	let order = [];

	function discover() {
		let classes;
		try {
			classes = JSON.parse(fs.readFileSync(path.join(root, MOD_JSON), 'utf8')).entrypoints['fabric-client-gametest'] || [];
		} catch (e) {
			return;
		}
		const items = [];
		order = [];
		for (const entry of classes) {
			const className = typeof entry === 'string' ? entry : entry.value;
			const simple = className.slice(className.lastIndexOf('.') + 1);
			if (!simple.endsWith(SUFFIX)) {
				continue;
			}
			const name = simple.slice(0, -SUFFIX.length);
			const file = path.join(root, SOURCES, ...className.split('.')) + '.java';
			const item = controller.createTestItem(name, name, fs.existsSync(file) ? vscode.Uri.file(file) : undefined);
			item.range = classLine(file, simple);
			item.sortText = name.toLowerCase();
			items.push(item);
			order.push(name);
		}
		controller.items.replace(items);
	}

	discover();
	const watcher = vscode.workspace.createFileSystemWatcher(new vscode.RelativePattern(folder, `{${MOD_JSON},${SOURCES}/**/*${SUFFIX}.java}`));
	watcher.onDidChange(discover);
	watcher.onDidCreate(discover);
	watcher.onDidDelete(discover);
	context.subscriptions.push(watcher);
	controller.refreshHandler = async () => discover();

	context.subscriptions.push(vscode.commands.registerCommand('cassonade.openScreenshots', () => {
		const dir = path.join(root, SCREENSHOTS);
		if (!fs.existsSync(dir)) {
			vscode.window.showInformationMessage('No screenshots yet: run a client test first.');
			return;
		}
		vscode.env.openExternal(vscode.Uri.file(dir));
	}));

	let running = false;
	const handler = debug => async (request, token) => {
		if (running) {
			vscode.window.showWarningMessage('Client tests are already running.');
			return;
		}
		const chosen = new Set();
		if (request.include) {
			request.include.forEach(item => chosen.add(item.id));
		} else {
			controller.items.forEach(item => {
				if (!FLAGS[item.id]) {
					chosen.add(item.id);
				}
			});
		}
		(request.exclude || []).forEach(item => chosen.delete(item.id));
		const names = order.filter(name => chosen.has(name));
		// One game for the plain tests, one each for those with a flag
		const batches = [];
		const plain = names.filter(name => !FLAGS[name]);
		if (plain.length) {
			batches.push(plain);
		}
		names.filter(name => FLAGS[name]).forEach(name => batches.push([name]));

		const run = controller.createTestRun(request);
		running = true;
		try {
			for (const batch of batches) {
				if (token.isCancellationRequested) {
					batch.forEach(name => run.skipped(controller.items.get(name)));
					continue;
				}
				await runBatch(batch, run, token, debug);
			}
		} finally {
			running = false;
			run.end();
		}
	};
	controller.createRunProfile('Run', vscode.TestRunProfileKind.Run, handler(false), true);
	// Gradle starts the game suspended until the debugger attaches, then breakpoints stop the test
	controller.createRunProfile('Debug', vscode.TestRunProfileKind.Debug, handler(true), true);

	function runBatch(batch, run, token, debug) {
		const items = batch.map(name => controller.items.get(name));
		items.forEach(item => run.started(item));
		const args = ['runClientGameTest', '-Ptests=' + batch.join(',')];
		if (FLAGS[batch[0]]) {
			args.push(FLAGS[batch[0]]);
		}
		if (debug) {
			args.push('--debug-jvm');
		}
		run.appendOutput(`> ./gradlew ${args.join(' ')}\r\n`);
		const start = Date.now();
		return new Promise(resolve => {
			const child = spawn(path.join(root, 'gradlew'), args, { cwd: root });
			let output = '';
			let attached = !debug;
			const append = data => {
				const text = data.toString();
				output += text;
				run.appendOutput(text.replace(/\r?\n/g, '\r\n'));
				const port = attached ? null : DEBUG_PORT.exec(output);
				if (port) {
					attached = true;
					vscode.debug.startDebugging(folder, {
						type: 'java',
						name: 'Cassonade client test',
						request: 'attach',
						hostName: 'localhost',
						port: Number(port[1]),
					}, { testRun: run });
				}
			};
			child.stdout.on('data', append);
			child.stderr.on('data', append);
			const cancel = token.onCancellationRequested(() => child.kill('SIGINT'));
			child.on('close', code => {
				cancel.dispose();
				const duration = batch.length === 1 ? Date.now() - start : undefined;
				const shots = screenshots(start);
				if (shots.length) {
					// Absolute paths, which the output panel turns into links
					const list = `\r\nScreenshots (${shots.length}):\r\n` + shots.map(shot => `  ${shot}\r\n`).join('');
					run.appendOutput(list, undefined, batch.length === 1 ? items[0] : undefined);
				}
				if (token.isCancellationRequested) {
					items.forEach(item => run.skipped(item));
				} else if (code === 0) {
					items.forEach(item => run.passed(item, duration));
				} else {
					report(stripAnsi(output), batch, items, run, duration, shots);
				}
				resolve();
			});
			child.on('error', error => {
				items.forEach(item => run.errored(item, new vscode.TestMessage(String(error))));
			});
		});
	}

	// The first failure stops the game: the tests before it passed, those after it did not run
	function report(output, batch, items, run, duration, shots) {
		const at = output.indexOf(FAILURE);
		if (at < 0) {
			// The build itself failed (compilation, Gradle): no test ran
			const message = new vscode.TestMessage(output.trim().split('\n').slice(-40).join('\n'));
			items.forEach(item => run.errored(item, message));
			return;
		}
		const trace = output.slice(at).split('\n').slice(0, 60);
		const frames = trace.map(line => /^\s*at (com\.cassonade\.test\.[\w.$]+)\.([\w$<>]+)\(([\w$]+\.java):(\d+)\)/.exec(line)).filter(Boolean);
		// The failing test is the one whose runTest the runner called: the deepest of the test frames
		const runTest = frames.filter(f => f[2] === 'runTest').map(f => f[1].slice(f[1].lastIndexOf('.') + 1).replace(/\$.*/, '')).pop();
		const failed = runTest && runTest.endsWith(SUFFIX) ? batch.indexOf(runTest.slice(0, -SUFFIX.length)) : -1;
		// The last screenshot shows the game as the test failed, or just before
		const last = shots.length ? `\n\nLast screenshot: ${shots[shots.length - 1]}` : '';
		const message = new vscode.TestMessage((trace.slice(1).join('\n').trim() || trace[0]) + last);
		const top = frames[0];
		if (top) {
			const pkg = top[1].slice(0, top[1].lastIndexOf('.')).split('.').filter(part => !/^[A-Z]/.test(part) && !part.includes('$'));
			const file = path.join(root, SOURCES, ...pkg, top[3]);
			if (fs.existsSync(file)) {
				const line = Number(top[4]) - 1;
				message.location = new vscode.Location(vscode.Uri.file(file), new vscode.Range(line, 0, line, 0));
			}
		}
		if (failed < 0) {
			items.forEach(item => run.failed(item, message));
			return;
		}
		items.forEach((item, i) => {
			if (i < failed) {
				run.passed(item);
			} else if (i === failed) {
				run.failed(item, message, duration);
			} else {
				run.skipped(item);
			}
		});
	}

	// The screenshots taken since the run started, in the order taken
	function screenshots(since) {
		const dir = path.join(root, SCREENSHOTS);
		try {
			return fs.readdirSync(dir)
				.filter(name => name.endsWith('.png'))
				.map(name => path.join(dir, name))
				.map(file => ({ file, time: fs.statSync(file).mtimeMs }))
				.filter(shot => shot.time >= since - 1000)
				.sort((a, b) => a.time - b.time)
				.map(shot => shot.file);
		} catch (e) {
			return [];
		}
	}
}

function classLine(file, simple) {
	try {
		const lines = fs.readFileSync(file, 'utf8').split('\n');
		const line = lines.findIndex(text => new RegExp(`\\bclass ${simple}\\b`).test(text));
		return line < 0 ? undefined : new vscode.Range(line, 0, line, 0);
	} catch (e) {
		return undefined;
	}
}

function stripAnsi(text) {
	return text.replace(/\x1b\[[0-9;]*m/g, '');
}

module.exports = { activate };
