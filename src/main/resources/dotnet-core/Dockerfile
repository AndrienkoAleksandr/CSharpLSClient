FROM eclipse/stack-base:ubuntu

RUN sudo sh -c 'echo "deb [arch=amd64] http://apt-mo.trafficmanager.net/repos/dotnet-release/ xenial main" > /etc/apt/sources.list.d/dotnetdev.list' && \
    sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 417A0893 && \
    sudo apt-get update && \
    sudo apt-get install -y dotnet-sdk-2.0.0-preview2-006497

RUN sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF && \
    echo "deb http://download.mono-project.com/repo/ubuntu beta-xenial main" | sudo tee /etc/apt/sources.list.d/mono-official-beta.list && \
    sudo apt-get update && \
    sudo apt-get install -y mono-devel

RUN wget -qO- https://deb.nodesource.com/setup_7.x | sudo -E bash -
RUN sudo apt update && sudo apt -y install nodejs

CMD tail -f /dev/null
