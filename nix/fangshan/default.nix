# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2024 Jiuyang Liu <liu@jiuyang.me>

{ lib, newScope, }:
lib.makeScope newScope (scope:
let
  designTarget = "FangShan";
  tbTarget = "FangShanTestBench";
  formalTarget = "FangShanFormal";
  dpiLibName = "fangshanemu";
in
{
  # RTL
  fangshan-compiled = scope.callPackage ./fangshan.nix { target = designTarget; };
  elaborate = scope.callPackage ./elaborate.nix {
    elaborator = scope.fangshan-compiled.elaborator;
  };
  mlirbc = scope.callPackage ./mlirbc.nix { };
  rtl = scope.callPackage ./rtl.nix { };

  # Testbench
  tb-compiled = scope.callPackage ./fangshan.nix { target = tbTarget; };
  tb-elaborate = scope.callPackage ./elaborate.nix {
    elaborator = scope.tb-compiled.elaborator;
  };
  tb-mlirbc =
    scope.callPackage ./mlirbc.nix { elaborate = scope.tb-elaborate; };
  tb-rtl = scope.callPackage ./rtl.nix { mlirbc = scope.tb-mlirbc; };
  tb-dpi-lib = scope.callPackage ./dpi-lib.nix { inherit dpiLibName; };

  verilated = scope.callPackage ./verilated.nix {
    rtl = scope.tb-rtl.override { enable-layers = [ "Verification" ]; };
    dpi-lib = scope.tb-dpi-lib;
  };
  verilated-trace = scope.verilated.override {
    dpi-lib = scope.verilated.dpi-lib.override { enable-trace = true; };
  };

  # Formal
  formal-compiled = scope.callPackage ./fangshan.nix { target = formalTarget; };
  formal-elaborate = scope.callPackage ./elaborate.nix {
    elaborator = scope.formal-compiled.elaborator;
  };
  formal-mlirbc =
    scope.callPackage ./mlirbc.nix { elaborate = scope.formal-elaborate; };
  formal-rtl = scope.callPackage ./rtl.nix {
    mlirbc = scope.formal-mlirbc;
    enable-layers = [ "Verification" "Verification.Assume" "Verification.Assert" "Verification.Cover" ];
  };
  jg-fpv = scope.callPackage ./jg-fpv.nix {
    rtl = scope.formal-rtl;
  };

  # TODO: designConfig should be read from OM
  tbConfig = with builtins;
    fromJSON (readFile ./../../configs/${tbTarget}.json);

})

