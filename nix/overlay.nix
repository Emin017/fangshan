# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2024 Jiuyang Liu <liu@jiuyang.me>

let
  getEnv' = key:
    let
      val = builtins.getEnv key;
    in
    if val == "" then
      builtins.throw "${key} not set or '--impure' not applied"
    else val;
in
final: prev:
let
  rv32Pkgs = final.pkgsCross.riscv32-embedded;
  rv32BuildPkgs = rv32Pkgs.buildPackages;
in
rec {
  espresso = final.callPackage ./pkgs/espresso.nix { };

  mill =
    let
      jre = final.jdk21;
      version = "0.11.12";
    in
    (prev.mill.override {
      inherit jre;
    }).overrideAttrs
      (_: {
        passthru = { inherit jre; };
        src = prev.fetchurl {
          url = "https://github.com/com-lihaoyi/mill/releases/download/${version}/${version}-assembly";
          hash = "sha256-k4/oMHvtq5YXY8hRlX4gWN16ClfjXEAn6mRIoEBHNJo=";
        };
      });

  fetchMillDeps = final.callPackage ./pkgs/mill-builder.nix { };

  circt-full = final.callPackage ./pkgs/circt-full.nix { };

  # faster strip-undetereminism
  add-determinism = final.callPackage ./pkgs/add-determinism { };

  projectDependencies = final.callPackage ./pkgs/project-dependencies.nix { };

  fangshan = final.callPackage ./fangshan { };

  rv32-cc =
    let
      libc = rv32Pkgs.stdenv.cc.libc.overrideAttrs (oldAttrs: {
        CFLAGS_FOR_TARGET = "-march=rv32gc -mabi=ilp32";
      });
      rv-cc = rv32Pkgs.stdenv.cc.cc.overrideAttrs (oldAttrs: {
        configureFlags = oldAttrs.configureFlags ++ [
          "--enable-multilib"
        ];
        CFLAGS_FOR_TARGET = "-march=rv32gc -mabi=ilp32";
      });
    in
    rv32BuildPkgs.wrapCCWith {
      cc = rv-cc;
      libc = libc;
      bintools = rv32Pkgs.stdenv.cc.bintools.override {
        inherit libc;
        inherit (rv32BuildPkgs.bintools) bintools;
      };
    };
  rv32-stdenv = (rv32Pkgs.overrideCC rv32Pkgs.stdenv rv32-cc);

  dummy = final.callPackage ../tests/dummy {
    stdenv = rv32-stdenv;
  };
}
