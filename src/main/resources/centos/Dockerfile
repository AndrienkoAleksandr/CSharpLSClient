# Copyright (c) 2012-2017 Red Hat, Inc
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html

FROM registry.centos.org/centos/centos

ARG JAVA_VERSION=1.8.0

RUN yum -y update && \
    yum -y install sudo openssh-server g++ gcc glibc-devel make git java-${JAVA_VERSION}-openjdk-devel && \
    yum -y install libunwind libicu && \
    yum clean all && \
    sed -ri 's/UsePAM yes/#UsePAM yes/g' /etc/ssh/sshd_config && \
    sed -ri 's/#UsePAM no/UsePAM no/g' /etc/ssh/sshd_config && \
    echo "%wheel ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers && \
    useradd -u 1000 -G users,wheel -d /home/user --shell /bin/bash -m user && \
    usermod -p "*" user && \
    sed -i 's/requiretty/!requiretty/g' /etc/sudoers

RUN curl -sSL -o dotnet.tar.gz https://aka.ms/dotnet-sdk-2.0.0-preview2-linux-x64-bin && \
    sudo mkdir -p /opt/dotnet && sudo tar zxf dotnet.tar.gz -C /opt/dotnet && \
    sudo ln -s /opt/dotnet/dotnet /usr/local/bin
# todo clean up dotnet installation ...

RUN sudo yum -y install epel-release && \
    sudo yum -y install nodejs

USER user

EXPOSE 5000