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
final: prev: {
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
}
