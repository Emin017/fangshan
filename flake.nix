# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2024 Jiuyang Liu <liu@jiuyang.me>
{
  description = "Chisel Nix";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    treefmt-nix.url = "github:numtide/treefmt-nix";
    treefmt-nix.inputs.nixpkgs.follows = "nixpkgs";
  };

  outputs =
    inputs@{ self
    , nixpkgs
    , flake-utils
    , treefmt-nix
    ,
    }:
    let
      overlay = import ./nix/overlay.nix;
    in
    {
      # System-independent attr
      inherit inputs;
      overlays.default = overlay;
    }
    // flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import nixpkgs {
          overlays = [ overlay ];
          inherit system;
        };
        treefmtEval = treefmt-nix.lib.evalModule pkgs {
          projectRootFile = ".git/config";
          programs = {
            nixpkgs-fmt.enable = true; # nix
            rustfmt.enable = true; # rust
            yamlfmt.enable = true; # yaml
            taplo.enable = true; # toml
          };
        };
      in
      {
        formatter = treefmtEval.config.build.wrapper;
        checks = {
          formatting = treefmtEval.config.build.check self;
        };
        legacyPackages = pkgs;
        devShells.default = pkgs.mkShell (
          {
            inputsFrom = [
              pkgs.fangshan.fangshan-compiled
              pkgs.fangshan.tb-dpi-lib
              pkgs.dummy
            ];
            nativeBuildInputs = [
              pkgs.cargo
              pkgs.rustfmt
              pkgs.rust-analyzer
              pkgs.verilator
            ];
            RUST_SRC_PATH = "${pkgs.rust.packages.stable.rustPlatform.rustLibSrc}";
            shellHook = ''
                mill mill.bsp.BSP/install
                export CHISEL_FIRTOOL_PATH=$CIRCT_INSTALL_PATH/bin
            '';
          }
          // pkgs.fangshan.tb-dpi-lib.env
          // pkgs.fangshan.fangshan-compiled.env
        );
      }
    );
}
