FROM ubuntu:16.04

RUN apt-get update && apt-get -y install sudo mc nano git maven default-jdk curl

RUN sudo su && curl -sL https://deb.nodesource.com/setup_6.x | sudo bash - \
    && apt-get update \
    && apt-get install -y nodejs

ENV DOTNET_SDK_VERSION 2.0.0-preview2-006497
ENV DOTNET_SDK_DOWNLOAD_URL https://dotnetcli.blob.core.windows.net/dotnet/Sdk/$DOTNET_SDK_VERSION/dotnet-sdk-$DOTNET_SDK_VERSION-linux-x64.tar.gz
ENV DOTNET_SDK_DOWNLOAD_SHA 0664FE726EB07650D9C036B0E5E6D33AA8B10DB89DADA4A5E85FB5757FD1FAE1570AF0D526484014976761C829B1E55A70EF9966EECE877A56C2C426090896EB

RUN curl -SL $DOTNET_SDK_DOWNLOAD_URL --output dotnet.tar.gz \
    && echo "$DOTNET_SDK_DOWNLOAD_SHA dotnet.tar.gz" | sha512sum -c - \
    && mkdir -p /usr/share/dotnet \
    && tar -zxf dotnet.tar.gz -C /usr/share/dotnet \
    && rm dotnet.tar.gz \
    && ln -s /usr/share/dotnet/dotnet /usr/bin/dotnet

CMD tail -f /dev/null