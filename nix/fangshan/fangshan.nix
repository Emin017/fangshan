# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2024 Jiuyang Liu <liu@jiuyang.me>

{ lib
, stdenv
, fetchMillDeps
, makeWrapper
, jdk21
, git

  # chisel deps
, mill
, espresso
, circt-full
, jextract-21
, add-determinism
, projectDependencies

, target
}:

let
  self = stdenv.mkDerivation rec {
    name = "fangshan";

    mainClass = "fangshan.elaborator.${target}Main";

    src = with lib.fileset;
      toSource {
        root = ./../..;
        fileset = unions [
          ./../../build.sc
          ./../../common.sc
          ./../../fangshan
          ./../../elaborator
        ];
      };

    passthru = {
      millDeps = fetchMillDeps {
        inherit name;
        src = with lib.fileset;
          toSource {
            root = ./../..;
            fileset = unions [ ./../../build.sc ./../../common.sc ];
          };
        millDepsHash =
          if stdenv.isDarwin then
            "sha256-rsSgRrhP6aWtytM5J1mhKK6mm1VQz3mpW9+62JcFuM4="
          else
            "sha256-rpthvEW8theczcymtWDAnMY/ZFr4TjoZocOxg3vjjfE="
        ;
        nativeBuildInputs = [ projectDependencies.setupHook ];
      };

      editable = self.overrideAttrs (_: {
        shellHook = ''
          setupSubmodulesEditable
          mill mill.bsp.BSP/install 0
        '';
      });

      inherit target;
      inherit env;
    };

    shellHook = ''
      setupSubmodules
    '';

    nativeBuildInputs = [
      mill
      circt-full
      jextract-21
      add-determinism
      espresso
      git

      makeWrapper
      passthru.millDeps.setupHook

      projectDependencies.setupHook
    ];

    env = {
      CIRCT_INSTALL_PATH = circt-full;
      JEXTRACT_INSTALL_PATH = jextract-21;
    };

    outputs = [ "out" "elaborator" ];

    meta.mainProgram = "elaborator";

    buildPhase = ''
      mill -i '__.assembly'
    '';

    installPhase = ''
      mkdir -p $out/share/java

      add-determinism -j $NIX_BUILD_CORES out/elaborator/assembly.dest/out.jar

      mv out/elaborator/assembly.dest/out.jar $out/share/java/elaborator.jar

      mkdir -p $elaborator/bin
      makeWrapper ${jdk21}/bin/java $elaborator/bin/elaborator \
        --add-flags "--enable-preview -Djava.library.path=${circt-full}/lib -cp $out/share/java/elaborator.jar ${mainClass}"
    '';
  };
in
self
