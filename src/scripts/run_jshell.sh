#!/bin/bash

# ========================================
# Helper Script - JShell SQL Parser
# ========================================

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  SQL Parser - JShell Helper Script${NC}"
echo -e "${BLUE}========================================${NC}\n"

# Verificar se Maven está instalado
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven não encontrado. Instale o Maven primeiro.${NC}"
    exit 1
fi

# Verificar se JShell está disponível
if ! command -v jshell &> /dev/null; then
    echo -e "${RED}❌ JShell não encontrado. Use Java 9 ou superior.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Maven e JShell encontrados${NC}\n"

# Compilar projeto
echo -e "${YELLOW}📦 Compilando projeto...${NC}"
mvn clean compile > /dev/null 2>&1

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Erro na compilação. Execute 'mvn compile' para ver detalhes.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Compilação concluída${NC}\n"

# Construir classpath
echo -e "${YELLOW}🔧 Construindo classpath...${NC}"
CLASSPATH="target/classes"

# Adicionar dependências do Maven
MAVEN_DEPS=$(mvn dependency:build-classpath -q -DincludeScope=compile -Dmdep.outputFile=/dev/stdout 2>/dev/null)
if [ ! -z "$MAVEN_DEPS" ]; then
    CLASSPATH="$CLASSPATH:$MAVEN_DEPS"
fi

echo -e "${GREEN}✅ Classpath configurado${NC}\n"

# Verificar se há script de teste
TEST_SCRIPT="test_queries.jsh"
if [ -f "$TEST_SCRIPT" ]; then
    echo -e "${BLUE}📋 Script de teste encontrado: $TEST_SCRIPT${NC}"
    echo -e "${YELLOW}Deseja executar os testes automaticamente? (s/n)${NC}"
    read -r response
    
    if [[ "$response" =~ ^([sS][iI][mM]|[sS])$ ]]; then
        echo -e "\n${GREEN}▶️  Executando testes...${NC}\n"
        jshell --class-path "$CLASSPATH" "$TEST_SCRIPT"
        exit 0
    fi
fi

# Iniciar JShell interativo
echo -e "\n${GREEN}🚀 Iniciando JShell interativo...${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}Comandos úteis:${NC}"
echo -e "  ${GREEN}/help${NC}     - Ajuda do JShell"
echo -e "  ${GREEN}/list${NC}     - Listar comandos executados"
echo -e "  ${GREEN}/exit${NC}     - Sair do JShell"
echo -e ""
echo -e "${YELLOW}Início rápido:${NC}"
echo -e "  ${GREEN}import lib.*;${NC}"
echo -e "  ${GREEN}import lib.parser.*;${NC}"
echo -e "  ${GREEN}UserQueryParser parser = new UserQueryParser();${NC}"
echo -e "  ${GREEN}String result = parser.execute(\"SELECT * FROM users\");${NC}"
echo -e "  ${GREEN}System.out.println(result);${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

jshell --class-path "$CLASSPATH"

echo -e "\n${GREEN}👋 JShell finalizado${NC}"
