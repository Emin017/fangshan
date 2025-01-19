#!/usr/bin/env swift

import Foundation

func createShell(_ command: String) throws -> String {
  let task = Process()
  let pipe = Pipe()
  var output = ""

  task.standardOutput = pipe
  task.standardError = pipe
  task.arguments = ["-c", command]
  task.executableURL = URL(fileURLWithPath: "/bin/zsh")
  task.standardInput = nil

  pipe.fileHandleForReading.readabilityHandler = { handle in
    let data = handle.availableData
    if let str = String(data: data, encoding: .utf8) {
        output = output + str
        print(str, terminator: "")
        fflush(stdout)
    }
  }

  try task.run()

  let data = pipe.fileHandleForReading.readDataToEndOfFile()
  pipe.fileHandleForReading.readabilityHandler = nil
  print(String(data: data, encoding: .utf8) ?? "")

  return output
}

func readJson(_ path: String) -> [String: Any] {
  let url = URL(fileURLWithPath: path)
  let data = try! Data(contentsOf: url)
  return try! JSONSerialization.jsonObject(with: data, options: []) as! [String: Any]
}

func buildDPI(buildDir: String, parameter: [String: Any]? = nil) {
  /// Build DPI library
  print("Building DPI")
  let cargoFlags = ["build", "--features=sv2023", "--features=trace"]

  let paramsMap = [
    "fangshanParameter.width": "DESIGN_DATA_WIDTH",
    "timeout": "DESIGN_TIMEOUT",
    "testSize": "DESIGN_TEST_SIZE",
    "testVerbatimParameter.clockFlipTick": "CLOCK_FLIP_TIME",
  ]

  /// Set the environment variables for the DPI build
  let setEnv =
    parameter?.flatMap { (key, value) -> [(String, Any)] in
      switch value {
      case let xs as [String: Any]:
        return xs.map { (k, v) in (key + "." + k, v) }
      default:
        return [(key, value)]
      }
    }.filter {
      key, _ in
      paramsMap.keys.contains(key)
    }
    .map {
      key, value in
      "\(paramsMap[key]!)=\(value)"
    }.joined(separator: " ") ?? ""

  _ = try? createShell("cd fangshanemu && \(setEnv) cargo \(cargoFlags.joined(separator: " "))")
}

func runCompile() {
  /// Run mill compile and assembly
  print("Running mill compile and assembly")
  _ = try? createShell("./mill -i __.assembly")
}

func runBuild(name: String, buildDir: String) {
  /// Compile the design
  runCompile()
  /// Elaborate the design
  let elaboratorFlags = [
    "-cp", "out/elaborator/assembly.dest/out.jar",
    "fangshan.elaborator.\(name)Main",
    "design",
    "--parameter",
    "./configs/\(name).json",
    "--target-dir",
    ".",
  ]
  print("Running \(name) elaborator")
  _ = try? createShell("java \(elaboratorFlags.joined(separator: String(" ")))")
}

func runEmit(name: String, emitDir: String) {
  /// Run firtool to emit verilog, firrtl and mlir files
  let firtoolFlags = [
    "\(name).fir",
    "--split-verilog",
    "--annotation-file=\(name).anno.json",
    "-o",
    "\(emitDir)/emit",
  ]
  _ = try? createShell("firtool \(firtoolFlags.joined(separator: " "))")
}

func runEmu(name: String, buildDir: String) {
  let emuDir = buildDir + "/emu"
  buildDPI(buildDir: buildDir, parameter: readJson("configs/\(name).json"))  // Build DPI before running emu
  /// Copy the DPI library to the build directory
  let (_, _) = (
    try? createShell("mkdir -p \(emuDir)"),
    try? createShell("cp fangshanemu/target/debug/libfangshanemu.a \(emuDir)")
  )

  let verilatorFlags = [
    "--trace-fst",
    "--timing",
    "--threads 1",
    "-Mdir \(emuDir)",
    "-O1",
    "--main", "--exe",
    "--cc",
    "-I\(buildDir)/emit",
    "-DVERILATOR=1",
    "-f \(buildDir)/emit/filelist.f",
    "--top FangShanTestBench",
    "libfangshanemu.a",
  ]

  print("Running emu")
  _ = try? createShell("verilator \(verilatorFlags.joined(separator: " "))")
  print("Building emu")
  _ = try? createShell("cd \(emuDir) && make -j`nproc` -f V\(name).mk V\(name)")
}

do {
  var (name, dirPath) = ("", "")
  print("Enter emit target:")
  print("1. FangShan\n")
  print("2. FangShanTestbench\n")
  print("Your choice:")
  let target = readLine(strippingNewline: true)
  switch target {
  case "1":
    name = "FangShan"
    dirPath = "./fangshan-sim-result"
    runBuild(name: name, buildDir: dirPath)
    runEmit(name: name, emitDir: dirPath)
  case "2":
    name = "FangShanTestBench"
    dirPath = "./fangshan-sim-result"
    runBuild(name: name, buildDir: dirPath)
    runEmit(name: name, emitDir: dirPath)
    runEmu(name: name, buildDir: dirPath)
  default:
    print("Invalid target")
    exit(1)
  }
}
