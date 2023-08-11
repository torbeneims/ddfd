{
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  inputs.flake-utils.url = "github:numtide/flake-utils";

  outputs = { self, nixpkgs, flake-utils }: flake-utils.lib.eachDefaultSystem (system:
    let
      pkgs = import nixpkgs {
          inherit system;
          config.permittedInsecurePackages = [
            "openssl-1.1.1v"
            "openssl-1.1.1u"
          ];
      };
    in
    {
      devShell = pkgs.mkShell {
        packages = with pkgs; [
          jdk11_headless
          maven
          spark
          (writeShellScriptBin "master" ''
            spark-class org.apache.spark.deploy.master.Master --host localhost --port 7077 --webui-port 8080
          '')
          (writeShellScriptBin "package" ''
            mvn clean package
          '')
          (writeShellScriptBin "client" ''
            export SPARK_WORKER_DIR=.
            spark-class org.apache.spark.deploy.worker.Worker spark://localhost:7077 --cores 4
          '')
          (writeShellScriptBin "run" ''
            java -ea -jar target/DDFDAlgorithm-1.2-SNAPSHOT.jar -i data/ncvoters.tsv --rhsignoremap 0 -t 8 --traversersperrhs 1 -s 0 -p true -h 984968711
          '')
        ];
      };
      packages = with pkgs; {
        spark-master = stdenv.mkDerivation {
          buildInputs = [ maven spark ];
          phases = [ "installPhase" ];
          installPhase = ''
            spark-class org.apache.spark.deploy.master.Master --host localhost --port 7077 --webui-port 8080
          '';
        };
        spark-client = stdenv.mkDerivation {
          buildInputs = [ maven spark ];
          phases = [ "installPhase" ];
          installPhase = ''
            spark-class org.apache.spark.deploy.master.Master --host localhost --port 7077 --webui-port 8080
          '';
        };
      };
    }
  );
}

