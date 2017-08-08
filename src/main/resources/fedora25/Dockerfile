FROM fedora:25

RUN dnf -y install sudo curl mc nano tar
RUN sudo su && curl --silent --location https://rpm.nodesource.com/setup_6.x | sudo bash -
RUN sudo dnf -y install nodejs;
RUN sudo dnf -y install libunwind libicu
RUN sudo su && curl -sSL -o dotnet.tar.gz https://go.microsoft.com/fwlink/?linkid=848830 && \
    mkdir -p /opt/dotnet && sudo tar zxf dotnet.tar.gz -C /opt/dotnet && \
    ln -s /opt/dotnet/dotnet /usr/local/bin

CMD tail -f /dev/null
