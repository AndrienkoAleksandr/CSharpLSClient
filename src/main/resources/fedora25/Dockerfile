FROM fedora:25

RUN dnf -y install sudo curl mc nano tar

RUN sudo su && curl --silent --location https://rpm.nodesource.com/setup_6.x | sudo bash - && \
    sudo dnf -y install nodejs;

RUN sudo dnf -y install libunwind libicu && \
    curl -sSL -o dotnet.tar.gz https://aka.ms/dotnet-sdk-2.0.0-preview2-linux-x64-bin && \
    sudo mkdir -p /opt/dotnet && sudo tar zxf dotnet.tar.gz -C /opt/dotnet && \
    sudo ln -s /opt/dotnet/dotnet /usr/local/bin

RUN sudo yum install -y yum-utils && \
    rpm --import "http://keyserver.ubuntu.com/pks/lookup?op=get&search=0x3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF" && \
    yum-config-manager --add-repo http://download.mono-project.com/repo/centos7-beta/ && \
    yum install -y mono-devel

CMD tail -f /dev/null
