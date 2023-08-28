{
  description = "Dataset gathering tool for DDFD evaluation";

  inputs.flake-utils.url = "github:numtide/flake-utils";

  outputs = { self, flake-utils, nixpkgs }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        config = {
          inherit system;
          config.allowUnfree = true; # Allow packages with unfree licenses (we've made some ourselves)
         # nix.useSandbox = false; # Allow internet access during build, which is usually forbidden in nix for reproducability but makes it hard to use maven
          #nix.impure = true; # Allow sources to change because hashes are not locked (kills reproducability); Doesn't work, idc

          config.permittedInsecurePackages = [
            "openssl-1.1.1u"
            "openssl-1.1.1v"
            "openssl-1.0.2u"
          ];
        };
        pkgs = import nixpkgs config;
        sparkpkgs = import (builtins.fetchGit {
          # Descriptive name to make the store path easier to identify
          name = "nixpkgs-for-spark-2.2.1";
          url = "https://github.com/NixOS/nixpkgs/";
          ref = "refs/heads/nixos-22.11";
          rev = "eec050f3955d803dbee1ea66706f0bca7aa7ff88";
        }) config;

        fromFileWithCommand = (installPhase: (pname: (version: (url: (sha256: (pkgs.stdenv.mkDerivation {
            inherit pname;
            inherit version;
            nativeBuildInputs = [ pkgs.unzip ];
            src = pkgs.fetchurl { inherit url; inherit sha256;};
            phases = [ "installPhase" ];
            inherit installPhase;
          }))))));
        fromFile = fromFileWithCommand "mkdir -p out && cp $src $out";
        fromZipFile = fromFileWithCommand "mkdir -p out && unzip $src && cp *.{csv,txt,tsv} $out";

        tpch_version = "3.0.1";

        shellInputs = (with pkgs; [
              maven
              jdk17_headless
              nodejs_20

              hyperfine
              fish
              zsh
              tldr
            #python311Packages.pyspark
              stress
              perf-tools

             (python311.withPackages(ps: with ps; [ requests numpy matplotlib pandas ]))

              (writeShellScriptBin "master" ''
                spark-class org.apache.spark.deploy.master.Master --host localhost --port 7078 --webui-port 8080
              '')
              (writeShellScriptBin "package" ''
                mvn clean package
              '')
              (writeShellScriptBin "client" ''
                export SPARK_WORKER_DIR=.
                spark-class org.apache.spark.deploy.worker.Worker spark://localhost:7078 --cores 4
              '')
              (writeShellScriptBin "run" ''
                java -ea -jar target/DDFDAlgorithm-1.2-SNAPSHOT.jar -i data/ncvoters.tsv --rhsignoremap 0 -t 8 --traversersperrhs 1 -s 0 -p true -h 984968711
              '')

              (writeShellScriptBin "evaluate_ncvoter_ddfd" ''
                java -ea -jar $ddfd -i $ncvoter --rhsignoremap 0 -t 8 --traversersperrhs 1 -s 0 -j 1 p
              '')

            ])
            ++ [ sparkpkgs.spark sparkpkgs.hadoop ];

      in with pkgs; rec {
        packages = with stdenv; rec {
          spark = sparkpkgs.spark;
          dbgen = mkDerivation {
            pname = "dbgen";
            version =  tpch_version;
            # Please download this tool from https://www.tpc.org/tpc_documents_current_versions/current_specifications5.asp (personal information required)
            # And add the file to git!
            src = ./D2F6FAE3-DCCC-488F-AA78-C42A710C34F0-TPC-H-Tool.zip;
            # GitHub source does not work. Could not figure out where stray 'orders.tbl' is or how to overwrite it :(
            #src = fetchurl {
            #  url = "https://github.com/electrum/tpch-dbgen/archive/32f1c1b92d1664dba542e927d23d86ffa57aa253.zip";
            #  sha256 = "sha256-VL3fL7/i7aMQmg9ZERFKK2FQmn5FyN45VF8aWxJMZyE=";
            #};

            nativeBuildInputs = [ unzip ];
            postPatch = ''
              unzip $src #&& cp TPC-H\ V3.0.1/dbgen . -r
              cd TPC-H\ V3.0.1/dbgen || echo "failed to cd. this is normal when pulling from github"
              # sed does not work as expected, passing arguments explicitly below
              #sed -i -e "s|(CC\\s*=).*|1 gcc|g" makefile.suite
              #sed -i -e "s|(DATABASE\\s*=).*|1 INFORMIX|g" makefile.suite
              #sed -i -e "s|(MACHINE\\s*=).*|1 LINUX|g" makefile.suite
              #sed -i -e "s|(WORKLOAD\\s*=).*|1 TPCH|g" makefile.suite
              cat makefile.suite
            '';

            installPhase = ''
              make -f makefile.suite CC=gcc DATABASE=INFORMIX MACHINE=LINUX WORKLOAD=TPCH || true
              #./dbgen -s 1 -v
              mkdir -p $out
              #sed 's/|\\$//' lineitem.tbl > $out/lineitem.csv # removing the stray | after each line
              cp -r . $out
            '';

            meta = with lib; {
              description = "TPC-H Benchmark Tool";
              license = licenses.unfree;
            };
          };

          tpch-lineitem = mkDerivation {
            pname = "tpch-lineitem";
            version = tpch_version;
            src = dbgen;
            installPhase = ''
              $src/dbgen -s 1 -v
              sed 's/|\\$//' lineitem.tbl > $out # removing the stray | after each line
            '';
          };

          uniprot = fromFile "uniprot" "hpi-1001r_223c" "https://hpi.de/fileadmin/user_upload/fachgebiete/naumann/projekte/repeatability/DataProfiling/FD_Datasets/uniprot_1001r_223c.csv" "sha256-GV2C3TV/2SutNsYcLvyNm0KiJPzVjaJf2iOV1fttX2c=";

          flight = fromFile "flight" "hpi-20_500k" "https://hpi.de/fileadmin/user_upload/fachgebiete/naumann/projekte/repeatability/Flight/flights_20_500k.csv" "sha256-Fu5PNEbpyUC9BBL1PB8nxCka5cxYSDl7PNLwgcxU1zE=";

          adult = fromFile "adult" "hpi" "https://hpi.de/fileadmin/user_upload/fachgebiete/naumann/projekte/repeatability/DataProfiling/FD_Datasets/adult.csv" "sha256-/N+W5DNEzacnREqopGfgBHUwknsnj6VMiFSBEDatkHo=";

          homicide = fromZipFile "homicide" "" "<missing url>" "sha256-o25vCOLkr9fGV4ljhLdPxAC85Bx6TuIeQ3XRnLJbEZk=";

          ncvoter_old = fromFile "ncvoter" "hpi-1001r19c" "https://hpi.de/fileadmin/user_upload/fachgebiete/naumann/projekte/repeatability/DataProfiling/FD_Datasets/ncvoter_1001r_19c.csv" "sha256-hShVQaoTlUgvtcTOkOsOTRENkzxdujJ8/m9/E47J7sU=";

          fd-reduced = fromZipFile "fdreduced" "hpi-30" "https://hpi.de/fileadmin/user_upload/fachgebiete/naumann/projekte/repeatability/DataProfiling/FD_Datasets/fd-reduced-30.zip" "sha256-JUAvNouxwiLnrtdPem9GtNKDjHWtGLTpAqiU/bMU0ZI=";

          ncvoter = fromZipFile "ncvoter" "ncbe-2023-08-24" "https://s3.amazonaws.com/dl.ncsbe.gov/data/ncvoter_Statewide.zip" "sha256-Lxn/L5GYyjXUtiK55dtTpiUWKU556Owi7fIcOHFcKS0=";

          datasets = mkDerivation {
            name = "datasets";
            src = "";
            inherit uniprot flight adult fd-reduced ncvoter;
            phases = [ "installPhase" ];
            installPhase = ''
              mkdir -p $out
              cp $uniprot $out/uniprot.csv
              cp $ncvoter $out/ncvoter.csv
              cp $adult $out/adult.csv
              cp $flight $out/flight.csv
              cp ${fd-reduced} $out/fd-reduced.csv
            '';
              #cp $homicide $out/homicide.csv
              #cp ${tpch-lineitem} $out/lineitem.csv
          };

          ddfd_old = mkDerivation {
            meta.broken = true;
            pname = "ddfd";
            version =  "2023.1";
            src = fetchGit {
              url = "git@gitlab.com:torbeneims/metanome-algorithms.git";
              ref = "ddfd";
            };

            nativeBuildInputs = [ jdk8_headless maven wget ];
            postPatch = ''
              wget google.com || exit 1
              cd $src/ddfd/ddfdAlgorithm
              mvn clean package
            '';

            installPhase = ''
              mv target/DDFD*.jar $out
            '';
          };

          metanome-cli = fromFile "metanome-cli" "1.2" "https://hpi.de/fileadmin/user_upload/fachgebiete/naumann/projekte/repeatability/DataProfiling/Metanome/metanome-cli-1.2-SNAPSHOT.jar" "sha256-MaUYQvla/BrNLZLIqgbgPB90xPrhqf4e3HhQ48PYQ1M=";

          hyfd = fromFile "hyfd" "1.2" "https://hpi.de/fileadmin/user_upload/fachgebiete/naumann/projekte/repeatability/DataProfiling/Metanome_Algorithms/HyFD-1.2-SNAPSHOT.jar" "sha256-ibcHGl4bOfA7d4kHlCgChfStsG1Pdc2TRmtpx/2rjcQ=";

          tane = fromFile "tane" "1.2" "https://hpi.de/fileadmin/user_upload/fachgebiete/naumann/projekte/repeatability/DataProfiling/Metanome_Algorithms/TANE-1.2-SNAPSHOT.jar" "sha256-WDHjIKOVzr4h3ZhPM+DTstVVmAYbFcKwqIOAVQKx+uU=";

          ducc = fromFile "ducc" "1.2" "https://hpi.de/fileadmin/user_upload/fachgebiete/naumann/projekte/repeatability/DataProfiling/Metanome_Algorithms/DUCC-1.2-SNAPSHOT.jar" "sha256-ayA9Hljv1IZOKQmImQ0/dXvf8nkOhItJfAqenguAGoE=";

          ddfd = fromFile "ddfd" "2023.1" "https://gitlab.com/torbeneims/metanome-algorithms/-/raw/ddfd/ddfd/ddfdAlgorithm/target/DDFDAlgorithm-1.2-SNAPSHOT.jar?ref_type=heads&inline=false" "sha256-HB2Y57NVmxP38JSrCXRRFSrSdM8CITuLF5e/ptB39iQ=";


          all = mkDerivation {
            name = "ddfd-evaluation-all";
            src = "";
            inherit datasets;
            inherit metanome-cli hyfd tane ducc ddfd;
            phases = [ "installPhase" ];
            installPhase = ''
              mkdir -p $out/{data,algorithms}
              cp $datasets/* $out/data
              cp $hyfd $out/algorithms/hyfd.jar
              cp $tane $out/algorithms/tane.jar
              cp $ducc $out/algorithms/ducc.jar
              cp $ddfd $out/algorithms/ddfd.jar
              cp ${metanome-cli} $out/algorithms/metanome-cli.jar

              echo "java -cp algorithms/metanome-cli.jar:algorithms/hyfd.jar de.metanome.cli.App --algorithm de.metanome.algorithms.hyfd.HyFD --file-key INPUT_GENERATOR --files \$1" > $out/run_hyfd.sh
              echo "echo broken! && exit 1 java -cp algorithms/metanome-cli.jar:algorithms/tane.jar de.metanome.cli.App --algorithm de.metanome.algorithms.tane.TaneAlgorithm --file-key INPUT_GENERATOR --files \$1" > $out/run_tane.sh
              echo "echo broken! && exit 1 java -cp algorithms/metanome-cli.jar:algorithms/ducc.jar de.metanome.cli.App --algorithm de.metanome.algorithms.ducc.DuccAlgorithm --file-key INPUT_GENERATOR --files \$1" > $out/run_ducc.sh
              echo "java -jar algorithms/ddfd.jar -i \$1" > $out/run_ddfd.sh
              chmod a+r $out/run*
            '';
              #cp $homicide $out/homicide.csv
              #cp ${tpch-lineitem} $out/lineitem.csv
          };
        };
        
        devShells = {
          default = mkShell {
            name = "ddfd-evaluation";
            buildInputs = shellInputs;
          };
          pyShell = mkShell {
            name = "ddfd-evaluation-for-python";
            buildInputs = shellInputs ++ (with pkgs; [
              python311Packages.pyspark
            ]);
          };
        };
    });
}
