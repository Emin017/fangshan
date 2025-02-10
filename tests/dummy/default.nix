{ pkgs
, stdenv
, ...
}:
let
  self = stdenv.mkDerivation rec {
    pname = "dummy";
    name = pname;

    CC = "${stdenv.targetPlatform.config}-gcc";
    CXX = "${stdenv.targetPlatform.config}-g++";
    OBJCOPY = "${stdenv.targetPlatform.config}-objcopy";

    NIX_CFLAGS_COMPILE = [
      "-O2"
      "-mabi=ilp32"
      "-march=rv32im"
      "-fno-PIC"
      "-ffreestanding"
      "-nostdlib"
    ];
    src = ./.;

    buildPhase = ''
      runHook preBuild

      $CC $pname.c -o $pname.elf
      $OBJCOPY -j .text -O binary $pname.elf $pname.bin

      runHook postBuild
    '';

    installPhase = ''
      runHook preInstall

      mkdir -p $out/bin
      cp ${pname}.elf $out/bin

      runHook postInstall
    '';
    dontFixup = true;
  };
in
self
