import { hexToRgba } from 'theme/hexToRgba';

const Colors = {
  neutral: {
    '0': '#FFFFFF',
    '3': '#f9fafa',
    '4': '#f0f0f0',
    '5': '#F1F2F3',
    '10': '#E3E6E8',
    '15': '#D5DADD',
    '20': '#C7CED1',
    '25': '#C4C4C4',
    '30': '#ABB5BA',
    '40': '#8F9CA3',
    '50': '#73848C',
    '60': '#5C6970',
    '70': '#454F54',
    '75': '#394246',
    '80': '#2F3639',
    '85': '#22282A',
    '87': '#1E2224',
    '90': '#171A1C',
    '95': '#0B0D0E',
    '100': '#000',
  },
  transparency: {
    '10': 'rgba(10, 10, 10, 0.1)',
    '20': 'rgba(0, 0, 0, 0.1)',
    '50': 'rgba(34, 41, 47, 0.5)',
  },
  green: {
    '10': '#D6F5E0',
    '15': '#C2F0D1',
    '30': '#85E0A3',
    '40': '#5CD685',
    '50': '#33CC66',
    '60': '#29A352',
    '70': '#34C759',
  },
  brand: {
    '0': '#FFFFFF',
    '3': '#F9FAFA',
    '5': '#F1F2F3',
    '10': '#E3E6E8',
    '15': '#D5DADD',
    '20': '#C7CED1',
    '30': '#ABB5BA',
    '40': '#8F9CA3',
    '50': '#73848C',
    '60': '#5C6970',
    '70': '#454F54',
    '80': '#2F3639',
    '85': '#22282A',
    '90': '#171A1C',
    '95': '#0B0D0E',
  },
  red: {
    '10': '#FAD1D1',
    '20': '#F5A3A3',
    '50': '#E51A1A',
    '52': '#E63B19',
    '55': '#CF1717',
    '60': '#B81414',
  },
  orange: {
    '10': '#BF83401A',
    '20': '#FF8D28',
    '100': '#FF9D00',
  },
  yellow: {
    '10': '#FFEECC',
    '20': '#FFDD57',
    '30': '#FFD439',
  },
  blue: {
    '10': '#e3f2fd',
    '20': '#bbdefb',
    '30': '#90caf9',
    '40': '#64b5f6',
    '45': '#5865F2',
    '50': '#5B67E3',
    '60': '#7A7AB8',
    '70': '#5959A6',
    '80': '#3E3E74',
  },
  clusterColorPicker: {
    transparent: 'transparent',
    gray: '#E3E6E8',
    red: '#E63B19',
    orange: '#FF9D00',
    lettuce: '#9DD926',
    green: '#33CC33',
    turquoise: '#40BF95',
    blue: '#1A5DE5',
    violet: '#6633CC',
    pink: '#D926D9',
  },
  clusterMenuBackgroundColor: {
    transparent: 'transparent',
    gray: hexToRgba('#808080', 0.1),
    red: hexToRgba('#BF4040', 0.1),
    orange: hexToRgba('#BF8340', 0.1),
    lettuce: hexToRgba('#93BF40', 0.1),
    green: hexToRgba('#40BF40', 0.1),
    turquoise: hexToRgba('#40BF95', 0.1),
    blue: hexToRgba('#406ABF', 0.1),
    violet: hexToRgba('#6A40BF', 0.1),
    pink: hexToRgba('#BF40BF', 0.1),
  },
};

const baseTheme = {
  defaultIconColor: Colors.neutral[50],
  auth_page: {
    backgroundColor: Colors.brand[0],
    fontFamily: 'Inter, sans-serif',
    header: {
      cellBorderColor: Colors.brand[10],
      LogoBgColor: Colors.brand[90],
      LogoTextColor: Colors.brand[0],
    },
    signIn: {
      titleColor: Colors.brand[90],
      errorMessage: {
        color: Colors.red[52],
      },
      label: {
        color: Colors.brand[70],
      },
      authCard: {
        borderRadius: '16px',
        borderColor: Colors.brand[10],
        backgroundColor: Colors.brand[0],
        serviceNamecolor: Colors.brand[90],
        serviceTextColor: Colors.brand[50],
      },
    },
    footer: {
      fontSize: '12px',
      span: {
        color: Colors.brand[70],
        fontWeight: 500,
      },
      p: {
        color: Colors.brand[50],
        fontWeight: 400,
      },
    },
    icons: {
      githubColor: Colors.brand[90],
    },
  },
  heading: {
    h1: {
      color: Colors.neutral[90],
    },
    h3: {
      color: Colors.neutral[50],
      fontSize: '14px',
    },
    h4: Colors.neutral[90],
    base: {
      fontFamily: 'Inter, sans-serif',
      fontStyle: 'normal',
      fontWeight: 500,
      color: Colors.neutral[100],
    },
    variants: {
      1: {
        fontSize: '20px',
        lineHeight: '32px',
      },
      2: {
        fontSize: '20px',
        lineHeight: '32px',
      },
      3: {
        fontSize: '16px',
        lineHeight: '24px',
        fontWeight: 400,
        marginBottom: '16px',
      },
      4: {
        fontSize: '14px',
        lineHeight: '20px',
        fontWeight: 500,
      },
      5: {
        fontSize: '12px',
        lineHeight: '16px',
      },
      6: {
        fontSize: '12px',
        lineHeight: '16px',
      },
    },
  },
  code: {
    backgroundColor: Colors.neutral[5],
    color: Colors.red[55],
  },
  checkbox: {
    label: Colors.neutral[50],
    hint: Colors.neutral[50],
  },
  breakpoints: {
    S: 768,
    M: 1024,
    L: 1440,
  },
  layout: {
    minWidth: '1200px',
    navBarWidth: '240px',
    navBarHeight: '51px',
    rightSidebarWidth: '70vw',
    filtersSidebarWidth: '300px',

    mainBackgroundColor: Colors.brand[0],
    stuffColor: Colors.neutral[5],
    stuffBorderColor: Colors.neutral[10],
    overlay: {
      backgroundColor: Colors.neutral[50],
    },
    socialLink: Colors.neutral[20],
  },
  alert: {
    color: {
      error: Colors.red[10],
      success: Colors.green[10],
      warning: Colors.yellow[10],
      info: Colors.neutral[10],
      loading: Colors.neutral[10],
      blank: Colors.neutral[10],
      custom: Colors.neutral[10],
    },
    shadow: Colors.transparency[20],
  },
  circularAlert: {
    color: {
      error: Colors.red[50],
      success: Colors.green[40],
      warning: Colors.yellow[10],
      info: Colors.neutral[10],
    },
  },
  connectEditWarning: Colors.yellow[10],
  lastestVersionItem: {
    metaDataLabel: {
      color: Colors.neutral[50],
    },
  },
  icons: {
    chevronDownIcon: Colors.neutral[0],
    editIcon: {
      normal: Colors.neutral[30],
      hover: Colors.neutral[90],
      active: Colors.neutral[100],
      border: Colors.neutral[10],
    },
    closeIcon: {
      normal: Colors.neutral[30],
      hover: Colors.neutral[90],
      active: Colors.neutral[100],
      border: Colors.neutral[10],
    },
    cancelIcon: Colors.neutral[30],
    autoIcon: Colors.neutral[95],
    fileIcon: Colors.neutral[90],
    clockIcon: Colors.neutral[90],
    arrowDownIcon: Colors.neutral[90],
    moonIcon: Colors.neutral[95],
    sunIcon: Colors.neutral[95],
    infoIcon: Colors.neutral[30],
    closeCircleIcon: Colors.neutral[30],
    deleteIcon: Colors.red[20],
    warningIcon: Colors.yellow[30],
    warningRedIcon: {
      rectFill: Colors.red[10],
      pathFill: Colors.red[50],
    },
    messageToggleIcon: {
      normal: Colors.brand[30],
      hover: Colors.brand[40],
      active: Colors.brand[50],
    },
    verticalElipsisIcon: Colors.neutral[50],
    liveIcon: {
      circleBig: Colors.red[10],
      circleSmall: Colors.red[50],
    },
    newFilterIcon: Colors.brand[50],
    closeModalIcon: Colors.neutral[25],
    savedIcon: Colors.brand[50],
    dropdownArrowIcon: Colors.neutral[50],
    github: {
      hover: Colors.neutral[90],
      active: Colors.neutral[70],
    },
    discord: {
      normal: Colors.neutral[20],
      hover: Colors.blue[45],
      active: '#B8BEF9',
    },
    producthunt: {
      normal: Colors.neutral[20],
      hover: '#FF6154',
      active: '#FFBDB8',
    },
    menuIcon: Colors.brand[70],
    filterIcon: {
      normal: Colors.brand[70],
    },
    ftsIcon: {
      normal: Colors.neutral[30],
      active: Colors.brand[70],
    },
  },
  textArea: {
    borderColor: {
      normal: Colors.neutral[30],
      hover: Colors.neutral[50],
      focus: Colors.neutral[70],
      disabled: Colors.neutral[10],
    },
    color: {
      placeholder: {
        normal: Colors.neutral[30],
        focus: {
          normal: 'transparent',
          readOnly: Colors.neutral[30],
        },
      },
      disabled: Colors.neutral[30],
      readOnly: Colors.neutral[90],
    },
    backgroundColor: {
      readOnly: Colors.neutral[5],
    },
  },
  tag: {
    backgroundColor: {
      green: Colors.green[10],
      gray: Colors.neutral[5],
      yellow: Colors.yellow[10],
      white: Colors.neutral[10],
      red: Colors.red[10],
      blue: Colors.blue[10],
      secondary: Colors.neutral[15],
    },
    color: Colors.neutral[90],
  },
  switch: {
    unchecked: Colors.brand[30],
    hover: Colors.neutral[40],
    checked: Colors.brand[90],
    circle: Colors.neutral[0],
    disabled: Colors.brand[10],
    checkedIcon: {
      backgroundColor: Colors.neutral[10],
    },
  },
  pageLoader: {
    borderColor: Colors.brand[90],
    borderBottomColor: Colors.neutral[0],
  },
  topicFormLabel: {
    color: Colors.neutral[50],
  },
  dangerZone: {
    borderColor: Colors.red[60],
    color: {
      title: Colors.red[50],
      warningMessage: Colors.neutral[50],
    },
  },
  configList: {
    color: Colors.neutral[30],
  },
  tooltip: {
    bg: Colors.neutral[80],
    text: Colors.neutral[0],
  },
  topicsList: {
    color: {
      normal: Colors.neutral[90],
      hover: Colors.neutral[50],
      active: Colors.neutral[90],
    },
    backgroundColor: {
      hover: Colors.neutral[5],
      active: Colors.neutral[10],
    },
  },
  statistics: {
    createdAtColor: Colors.neutral[50],
    progressPctColor: Colors.neutral[100],
  },
  progressBar: {
    backgroundColor: Colors.neutral[3],
    compleatedColor: Colors.green[40],
    borderColor: Colors.neutral[10],
  },
  clusterConfigForm: {
    inputHintText: {
      secondary: Colors.neutral[60],
    },
    groupField: {
      backgroundColor: Colors.neutral[3],
    },
    fileInput: {
      color: Colors.neutral[85],
    },
  },
  lag: {
    down: Colors.green[70],
    up: Colors.orange[20],
    same: Colors.neutral[90],
    none: Colors.neutral[90],
  },
};

export const theme = {
  ...baseTheme,
  alertBadge: {
    background: Colors.orange[10],
    content: {
      color: Colors.neutral[90],
    },
    icon: {
      color: Colors.orange[100],
    },
  },
  kafkaConectClusters: {
    statistics: {
      background: Colors.neutral[5],
    },
    statistic: {
      background: Colors.neutral[0],
      count: {
        color: Colors.neutral[90],
      },
      header: {
        color: Colors.neutral[50],
      },
    },
  },
  logo: {
    color: Colors.brand[90],
  },
  version: {
    currentVersion: {
      color: Colors.neutral[30],
    },
    commitLink: {
      color: Colors.neutral[80],
    },
  },
  default: {
    color: {
      normal: Colors.neutral[90],
    },
    backgroundColor: Colors.brand[0],
    transparentColor: 'transparent',
  },
  link: {
    color: Colors.brand[50],
    hoverColor: Colors.brand[60],
  },
  user: {
    color: Colors.brand[70],
    hoverColor: Colors.brand[50],
  },
  hr: {
    backgroundColor: Colors.neutral[5],
  },
  pageHeading: {
    height: '64px',
    dividerColor: Colors.neutral[30],
    title: {
      color: Colors.brand[50],
    },
    backLink: {
      color: {
        normal: Colors.brand[70],
        hover: Colors.brand[60],
      },
    },
  },
  panelColor: {
    borderTop: 'none',
  },
  dropdown: {
    backgroundColor: Colors.brand[0],
    borderColor: Colors.brand[10],
    shadow: Colors.transparency[20],
    item: {
      color: {
        normal: Colors.neutral[90],
        danger: Colors.red[60],
      },
      backgroundColor: {
        default: Colors.neutral[0],
        hover: Colors.neutral[5],
      },
    },
  },
  ksqlDb: {
    query: {
      editor: {
        readonly: {
          background: Colors.neutral[3],
        },
        activeLine: {
          backgroundColor: Colors.neutral[5],
        },
        cell: {
          backgroundColor: Colors.neutral[10],
        },
        layer: {
          backgroundColor: Colors.neutral[5],
        },
        cursor: Colors.neutral[90],
        variable: Colors.red[50],
        aceString: Colors.green[60],
        codeMarker: Colors.yellow[20],
      },
    },
  },
  button: {
    primary: {
      backgroundColor: {
        normal: Colors.brand[80],
        hover: Colors.brand[90],
        active: Colors.brand[70],
        disabled: Colors.brand[50],
      },
      color: {
        normal: Colors.brand[0],
        hover: Colors.brand[0],
        active: Colors.brand[0],
        disabled: Colors.brand[30],
      },
    },
    secondary: {
      backgroundColor: {
        normal: Colors.brand[5],
        hover: Colors.brand[10],
        active: Colors.brand[15],
        disabled: Colors.brand[5],
      },
      color: {
        normal: Colors.brand[90],
        hover: Colors.brand[90],
        active: Colors.brand[90],
        disabled: Colors.brand[30],
      },
    },
    danger: {
      backgroundColor: {
        normal: Colors.red[50],
        hover: Colors.red[55],
        active: Colors.red[60],
        disabled: Colors.red[20],
      },
      color: {
        normal: Colors.brand[0],
        hover: Colors.brand[0],
        active: Colors.brand[0],
        disabled: Colors.red[10],
      },
    },
    text: {
      backgroundColor: {
        normal: Colors.brand[0],
        hover: Colors.brand[0],
        active: Colors.brand[0],
        disabled: Colors.brand[0],
      },
      color: {
        normal: Colors.brand[70],
        hover: Colors.brand[90],
        active: Colors.brand[50],
        disabled: Colors.brand[40],
      },
    },
    height: {
      S: '24px',
      M: '32px',
      L: '40px',
    },
    fontSize: {
      S: '14px',
      M: '14px',
      L: '16px',
    },
  },
  chips: {
    backgroundColor: {
      normal: Colors.neutral[5],
      hover: Colors.neutral[10],
      active: Colors.neutral[50],
      hoverActive: Colors.neutral[60],
    },
    color: {
      normal: Colors.neutral[70],
      hover: Colors.neutral[70],
      active: Colors.neutral[0],
      hoverActive: Colors.neutral[0],
    },
  },
  menu: {
    header: {
      backgroundColor: Colors.brand[0],
    },
    primary: {
      backgroundColor: {
        normal: 'transparent',
        hover: 'transparent',
        active: 'transparent',
      },
      color: {
        normal: Colors.neutral[50],
        hover: Colors.neutral[90],
        active: Colors.neutral[95],
      },
      statusIconColor: {
        online: Colors.green[40],
        offline: Colors.red[50],
        initializing: Colors.yellow[20],
      },
      chevronIconColor: hexToRgba(Colors.brand[95], 0.5),
      fontWeight: 500,
    },
    secondary: {
      backgroundColor: {
        normal: hexToRgba(Colors.brand[95], 0),
        hover: hexToRgba(Colors.brand[95], 0.03),
        active: hexToRgba(Colors.brand[95], 0.05),
      },
      color: {
        normal: Colors.brand[50],
        hover: Colors.brand[90],
        active: Colors.brand[90],
      },
      fontWeight: 400,
    },
  },
  clusterMenu: {
    backgroundColor: Colors.clusterMenuBackgroundColor,
  },
  schema: {
    backgroundColor: {
      tr: Colors.neutral[5],
      div: Colors.neutral[0],
      p: Colors.neutral[80],
      textarea: Colors.neutral[3],
    },
  },
  modal: {
    color: Colors.neutral[80],
    backgroundColor: Colors.neutral[0],
    border: {
      top: Colors.neutral[5],
      bottom: Colors.neutral[5],
      contrast: Colors.neutral[30],
    },
    overlay: Colors.transparency[10],
    shadow: Colors.transparency[20],
    contentColor: Colors.neutral[70],
  },
  confirmModal: {
    backgroundColor: Colors.neutral[0],
  },
  table: {
    actionBar: {
      backgroundColor: Colors.neutral[0],
    },
    th: {
      backgroundColor: {
        normal: Colors.neutral[0],
      },
      color: {
        sortable: Colors.neutral[30],
        normal: Colors.neutral[60],
        hover: Colors.brand[50],
        active: Colors.brand[50],
      },
      previewColor: {
        normal: Colors.brand[50],
      },
    },
    td: {
      borderTop: Colors.neutral[5],
      color: {
        normal: Colors.neutral[90],
      },
    },
    tr: {
      backgroundColor: {
        normal: Colors.neutral[0],
        hover: Colors.neutral[5],
      },
    },
    link: {
      color: {
        normal: Colors.neutral[90],
        hover: Colors.neutral[50],
        active: Colors.neutral[90],
      },
    },
    colored: {
      color: {
        attention: Colors.red[50],
        warning: Colors.yellow[20],
      },
    },
    expander: {
      normal: Colors.brand[30],
      hover: Colors.brand[40],
      active: Colors.brand[50],
      disabled: Colors.neutral[10],
    },
    pagination: {
      button: {
        background: Colors.neutral[90],
        border: Colors.neutral[80],
      },
      info: Colors.neutral[90],
    },
    filter: {
      multiSelect: {
        value: {
          color: Colors.neutral[90],
        },
        closeIcon: {},
        filterIcon: {
          fill: {
            normal: Colors.neutral[30],
            active: Colors.neutral[90],
            hover: Colors.neutral[90],
          },
        },
      },
    },
    resizer: {
      background: {
        normal: Colors.neutral[30],
        active: Colors.neutral[90],
        hover: Colors.neutral[90],
      },
    },
  },
  primaryTab: {
    height: '41px',
    color: {
      normal: Colors.neutral[50],
      hover: Colors.neutral[90],
      active: Colors.neutral[90],
      disabled: Colors.neutral[20],
    },
    borderColor: {
      active: Colors.brand[50],
      nav: Colors.neutral[5],
    },
  },
  secondaryTab: {
    backgroundColor: {
      normal: Colors.neutral[0],
      hover: Colors.neutral[5],
      active: Colors.neutral[10],
    },
    color: {
      normal: Colors.neutral[50],
      hover: Colors.neutral[90],
      active: Colors.neutral[90],
    },
  },
  select: {
    backgroundColor: {
      normal: Colors.neutral[0],
      hover: Colors.neutral[5],
      active: Colors.neutral[10],
    },
    color: {
      normal: Colors.neutral[70],
      hover: Colors.neutral[90],
      active: Colors.neutral[90],
      disabled: Colors.neutral[30],
    },
    borderColor: {
      normal: Colors.neutral[30],
      hover: Colors.neutral[50],
      active: Colors.neutral[70],
      disabled: Colors.neutral[10],
    },
    optionList: {
      borderColor: Colors.neutral[10],
      scrollbar: {
        backgroundColor: Colors.neutral[30],
      },
    },
    multiSelectOption: {
      checkbox: {
        backgroundColor: Colors.neutral[0],
        borderColor: Colors.neutral[50],
      },
    },
    label: Colors.neutral[50],
  },
  input: {
    borderColor: {
      normal: Colors.neutral[30],
      hover: Colors.neutral[50],
      focus: Colors.neutral[70],
      disabled: Colors.neutral[10],
    },
    color: {
      normal: Colors.neutral[90],
      placeholder: {
        normal: Colors.neutral[30],
        readOnly: Colors.neutral[30],
      },
      disabled: Colors.neutral[30],
      readOnly: Colors.neutral[90],
    },
    backgroundColor: {
      normal: Colors.neutral[0],
      readOnly: Colors.neutral[5],
      disabled: Colors.neutral[0],
    },
    error: Colors.red[50],
    icon: {
      color: Colors.neutral[70],
      hover: Colors.neutral[90],
    },
    label: {
      color: Colors.neutral[70],
    },
  },
  metrics: {
    backgroundColor: Colors.neutral[5],
    sectionTitle: Colors.neutral[90],
    indicator: {
      titleColor: Colors.neutral[50],
      warningTextColor: Colors.red[50],
      lightTextColor: Colors.neutral[30],
    },
    wrapper: Colors.neutral[0],
    filters: {
      color: {
        icon: Colors.neutral[90],
        normal: Colors.neutral[50],
      },
    },
  },
  scrollbar: {
    trackColor: {
      normal: Colors.neutral[5],
      active: Colors.neutral[5],
    },
    thumbColor: {
      normal: Colors.neutral[15],
      active: Colors.neutral[50],
    },
  },
  consumerTopicContent: {
    td: {
      backgroundColor: Colors.neutral[5],
    },
  },
  topicMetaData: {
    backgroundColor: Colors.neutral[5],
    color: {
      label: Colors.neutral[50],
      value: Colors.neutral[80],
      meta: Colors.neutral[30],
    },
    liderReplica: {
      color: Colors.green[60],
    },
    outOfSync: {
      color: Colors.red[50],
    },
  },
  viewer: {
    wrapper: {
      backgroundColor: Colors.neutral[3],
      color: Colors.neutral[80],
    },
  },
  activeFilter: {
    color: Colors.neutral[70],
    backgroundColor: Colors.neutral[5],
  },
  savedFilter: {
    filterName: Colors.neutral[90],
    color: Colors.neutral[30],
  },
  editFilter: {
    textColor: Colors.brand[50],
    deleteIconColor: Colors.brand[50],
  },
  acl: {
    table: {
      deleteIcon: Colors.neutral[50],
    },
    create: {
      radioButtons: {
        green: {
          normal: {
            background: Colors.neutral[0],
            text: Colors.neutral[50],
            border: Colors.neutral[10],
          },
          active: {
            background: Colors.green[50],
            text: Colors.neutral[0],
            border: Colors.green[50],
          },
          hover: {
            background: Colors.green[50],
            text: Colors.neutral[0],
            border: Colors.green[50],
          },
        },
        gray: {
          normal: {
            background: Colors.neutral[0],
            text: Colors.neutral[50],
            border: Colors.neutral[10],
          },
          active: {
            background: Colors.neutral[10],
            text: Colors.neutral[90],
            border: Colors.neutral[10],
          },
          hover: {
            background: Colors.neutral[10],
            text: Colors.neutral[50],
            border: Colors.neutral[10],
          },
        },
        red: {
          normal: {
            background: Colors.neutral[0],
            text: Colors.neutral[50],
            border: Colors.neutral[10],
          },
          active: {
            background: Colors.red[50],
            text: Colors.neutral[0],
            border: Colors.red[50],
          },
          hover: {
            background: Colors.red[50],
            text: Colors.neutral[0],
            border: Colors.red[50],
          },
        },
      },
    },
  },
  clusterColorPicker: {
    backgroundColor: Colors.clusterColorPicker,
    outline: Colors.brand[80],
    transparentCircle: {
      border: Colors.brand[10],
      cross: Colors.brand[30],
    },
  },
};

export type ThemeType = typeof theme;
export type ClusterColorKey =
  keyof ThemeType['clusterColorPicker']['backgroundColor'];

export const darkTheme: ThemeType = {
  ...baseTheme,
  alertBadge: {
    background: Colors.orange[10],
    content: {
      color: Colors.neutral[0],
    },
    icon: {
      color: Colors.orange[100],
    },
  },
  kafkaConectClusters: {
    statistics: {
      background: Colors.neutral[95],
    },
    statistic: {
      background: Colors.neutral[90],
      count: {
        color: Colors.neutral[0],
      },
      header: {
        color: Colors.neutral[50],
      },
    },
  },
  auth_page: {
    backgroundColor: Colors.neutral[90],
    fontFamily: baseTheme.auth_page.fontFamily,
    header: {
      cellBorderColor: Colors.brand[80],
      LogoBgColor: Colors.brand[0],
      LogoTextColor: Colors.brand[90],
    },
    signIn: {
      ...baseTheme.auth_page.signIn,
      titleColor: Colors.brand[0],
      label: {
        color: Colors.brand[30],
      },
      authCard: {
        ...baseTheme.auth_page.signIn.authCard,
        borderColor: Colors.brand[80],
        backgroundColor: Colors.brand[85],
        serviceNamecolor: Colors.brand[0],
      },
    },
    footer: {
      ...baseTheme.auth_page.footer,
      span: {
        color: Colors.brand[10],
        fontWeight: 500,
      },
    },
    icons: {
      githubColor: Colors.brand[0],
    },
  },
  logo: {
    color: '#FDFDFD',
  },
  version: {
    currentVersion: {
      color: Colors.neutral[50],
    },
    commitLink: {
      color: Colors.neutral[10],
    },
  },
  default: {
    color: {
      normal: Colors.neutral[0],
    },
    backgroundColor: Colors.brand[90],
    transparentColor: 'transparent',
  },
  link: {
    color: Colors.brand[50],
    hoverColor: Colors.brand[30],
  },
  user: {
    color: Colors.brand[20],
    hoverColor: Colors.brand[50],
  },
  hr: {
    backgroundColor: Colors.neutral[80],
  },
  pageHeading: {
    height: '64px',
    dividerColor: Colors.neutral[50],
    title: {
      color: Colors.brand[50],
    },
    backLink: {
      color: {
        normal: Colors.brand[30],
        hover: Colors.brand[15],
      },
    },
  },
  panelColor: {
    borderTop: Colors.neutral[80],
  },
  dropdown: {
    backgroundColor: Colors.brand[85],
    borderColor: Colors.brand[70],
    shadow: Colors.transparency[20],
    item: {
      color: {
        normal: Colors.neutral[0],
        danger: Colors.red[60],
      },
      backgroundColor: {
        default: Colors.neutral[85],
        hover: Colors.neutral[80],
      },
    },
  },
  ksqlDb: {
    query: {
      editor: {
        readonly: {
          background: Colors.neutral[3],
        },
        activeLine: {
          backgroundColor: Colors.neutral[80],
        },
        cell: {
          backgroundColor: Colors.neutral[75],
        },
        layer: {
          backgroundColor: Colors.neutral[80],
        },
        cursor: Colors.neutral[0],
        variable: Colors.red[50],
        aceString: Colors.green[60],
        codeMarker: Colors.yellow[20],
      },
    },
  },
  button: {
    primary: {
      backgroundColor: {
        normal: Colors.brand[10],
        hover: Colors.brand[0],
        active: Colors.brand[20],
        disabled: Colors.brand[50],
      },
      color: {
        normal: Colors.brand[90],
        hover: Colors.brand[90],
        active: Colors.brand[90],
        disabled: Colors.brand[70],
      },
    },
    secondary: {
      backgroundColor: {
        normal: Colors.brand[80],
        hover: Colors.brand[70],
        active: Colors.brand[60],
        disabled: Colors.brand[80],
      },
      color: {
        normal: Colors.brand[0],
        hover: Colors.brand[0],
        active: Colors.brand[0],
        disabled: Colors.brand[70],
      },
    },
    danger: {
      backgroundColor: {
        normal: Colors.red[50],
        hover: Colors.red[55],
        active: Colors.red[60],
        disabled: Colors.red[20],
      },
      color: {
        normal: Colors.brand[0],
        hover: Colors.brand[0],
        active: Colors.brand[0],
        disabled: Colors.red[10],
      },
    },
    text: {
      backgroundColor: {
        normal: 'transparent',
        hover: 'transparent',
        active: 'transparent',
        disabled: 'transparent',
      },
      color: {
        normal: Colors.brand[10],
        hover: Colors.brand[0],
        active: Colors.brand[20],
        disabled: Colors.brand[30],
      },
    },
    height: {
      S: '24px',
      M: '32px',
      L: '40px',
    },
    fontSize: {
      S: '14px',
      M: '14px',
      L: '16px',
    },
  },
  chips: {
    backgroundColor: {
      normal: Colors.neutral[80],
      hover: Colors.neutral[70],
      active: Colors.neutral[50],
      hoverActive: Colors.neutral[40],
    },
    color: {
      normal: Colors.neutral[0],
      hover: Colors.neutral[0],
      active: Colors.neutral[90],
      hoverActive: Colors.neutral[90],
    },
  },
  menu: {
    header: {
      backgroundColor: Colors.brand[90],
    },
    primary: {
      backgroundColor: {
        normal: 'transparent',
        hover: 'transparent',
        active: 'transparent',
      },
      color: {
        normal: Colors.brand[50],
        hover: Colors.brand[0],
        active: Colors.brand[20],
      },
      statusIconColor: {
        online: Colors.green[40],
        offline: Colors.red[50],
        initializing: Colors.yellow[20],
      },
      chevronIconColor: hexToRgba(Colors.brand[0], 0.5),
      fontWeight: 500,
    },
    secondary: {
      backgroundColor: {
        normal: hexToRgba(Colors.brand[0], 0),
        hover: hexToRgba(Colors.brand[0], 0.05),
        active: hexToRgba(Colors.brand[0], 0.1),
      },
      color: {
        normal: Colors.brand[40],
        hover: Colors.brand[0],
        active: Colors.brand[0],
      },
      fontWeight: 400,
    },
  },
  clusterMenu: {
    backgroundColor: Colors.clusterMenuBackgroundColor,
  },
  schema: {
    backgroundColor: {
      tr: Colors.neutral[5],
      div: Colors.neutral[0],
      p: Colors.neutral[0],
      textarea: Colors.neutral[85],
    },
  },
  modal: {
    color: Colors.neutral[0],
    backgroundColor: Colors.neutral[85],
    border: {
      top: Colors.neutral[75],
      bottom: Colors.neutral[75],
      contrast: Colors.neutral[75],
    },
    overlay: Colors.transparency[10],
    shadow: Colors.transparency[20],
    contentColor: Colors.neutral[30],
  },
  confirmModal: {
    backgroundColor: Colors.neutral[80],
  },
  table: {
    actionBar: {
      backgroundColor: Colors.neutral[90],
    },
    th: {
      backgroundColor: {
        normal: Colors.neutral[90],
      },
      color: {
        sortable: Colors.neutral[30],
        normal: Colors.neutral[60],
        hover: Colors.brand[50],
        active: Colors.brand[50],
      },
      previewColor: {
        normal: Colors.brand[50],
      },
    },
    td: {
      borderTop: Colors.neutral[80],
      color: {
        normal: Colors.neutral[0],
      },
    },
    tr: {
      backgroundColor: {
        normal: Colors.neutral[90],
        hover: Colors.neutral[85],
      },
    },
    link: {
      color: {
        normal: Colors.neutral[0],
        hover: Colors.neutral[0],
        active: Colors.neutral[0],
      },
    },
    colored: {
      color: {
        attention: Colors.red[50],
        warning: Colors.yellow[20],
      },
    },
    expander: {
      normal: Colors.brand[30],
      hover: Colors.brand[40],
      active: Colors.brand[50],
      disabled: Colors.neutral[10],
    },
    pagination: {
      button: {
        background: Colors.neutral[90],
        border: Colors.neutral[80],
      },
      info: Colors.neutral[0],
    },
    filter: {
      multiSelect: {
        value: {
          color: Colors.neutral[10],
        },
        closeIcon: {},
        filterIcon: {
          fill: {
            normal: Colors.neutral[50],
            hover: Colors.neutral[0],
            active: Colors.brand[30],
          },
        },
      },
    },
    resizer: {
      background: {
        normal: Colors.neutral[50],
        hover: Colors.neutral[0],
        active: Colors.brand[30],
      },
    },
  },
  primaryTab: {
    height: '41px',
    color: {
      normal: Colors.neutral[50],
      hover: Colors.neutral[0],
      active: Colors.brand[30],
      disabled: Colors.neutral[75],
    },
    borderColor: {
      active: Colors.brand[50],
      nav: Colors.neutral[80],
    },
  },
  secondaryTab: {
    backgroundColor: {
      normal: Colors.neutral[90],
      hover: Colors.neutral[85],
      active: Colors.neutral[80],
    },
    color: {
      normal: Colors.neutral[50],
      hover: Colors.neutral[0],
      active: Colors.neutral[0],
    },
  },
  switch: {
    unchecked: Colors.brand[30],
    hover: Colors.neutral[40],
    checked: Colors.brand[70],
    circle: Colors.neutral[0],
    disabled: Colors.brand[10],
    checkedIcon: {
      backgroundColor: Colors.neutral[10],
    },
  },
  select: {
    backgroundColor: {
      normal: Colors.neutral[85],
      hover: Colors.neutral[80],
      active: Colors.neutral[70],
    },
    color: {
      normal: Colors.neutral[20],
      hover: Colors.neutral[0],
      active: Colors.neutral[0],
      disabled: Colors.neutral[60],
    },
    borderColor: {
      normal: Colors.neutral[70],
      hover: Colors.neutral[50],
      active: Colors.neutral[70],
      disabled: Colors.neutral[70],
    },
    optionList: {
      borderColor: Colors.neutral[70],
      scrollbar: {
        backgroundColor: Colors.neutral[30],
      },
    },
    multiSelectOption: {
      checkbox: {
        backgroundColor: Colors.neutral[90],
        borderColor: Colors.neutral[50],
      },
    },
    label: Colors.neutral[50],
  },
  input: {
    borderColor: {
      normal: Colors.neutral[70],
      hover: Colors.neutral[50],
      focus: Colors.neutral[0],
      disabled: Colors.neutral[80],
    },
    color: {
      normal: Colors.neutral[0],
      placeholder: {
        normal: Colors.neutral[60],
        readOnly: Colors.neutral[0],
      },
      disabled: Colors.neutral[80],
      readOnly: Colors.neutral[0],
    },
    backgroundColor: {
      normal: Colors.neutral[90],
      readOnly: Colors.neutral[80],
      disabled: Colors.neutral[90],
    },
    error: Colors.red[50],
    icon: {
      color: Colors.neutral[30],
      hover: Colors.neutral[0],
    },
    label: {
      color: Colors.neutral[30],
    },
  },
  metrics: {
    backgroundColor: Colors.neutral[95],
    sectionTitle: Colors.neutral[0],
    indicator: {
      titleColor: Colors.neutral[0],
      warningTextColor: Colors.red[50],
      lightTextColor: Colors.neutral[60],
    },
    wrapper: Colors.neutral[0],
    filters: {
      color: {
        icon: Colors.neutral[0],
        normal: Colors.neutral[50],
      },
    },
  },
  scrollbar: {
    trackColor: {
      normal: Colors.neutral[90],
      active: Colors.neutral[85],
    },
    thumbColor: {
      normal: Colors.neutral[75],
      active: Colors.neutral[50],
    },
  },
  consumerTopicContent: {
    td: {
      backgroundColor: Colors.neutral[95],
    },
  },
  topicMetaData: {
    backgroundColor: Colors.neutral[90],
    color: {
      label: Colors.neutral[50],
      value: Colors.neutral[0],
      meta: Colors.neutral[60],
    },
    liderReplica: {
      color: Colors.green[60],
    },
    outOfSync: {
      color: Colors.red[50],
    },
  },
  viewer: {
    wrapper: {
      backgroundColor: Colors.neutral[85],
      color: Colors.neutral[0],
    },
  },
  activeFilter: {
    color: Colors.neutral[0],
    backgroundColor: Colors.neutral[80],
  },
  savedFilter: {
    filterName: Colors.neutral[0],
    color: Colors.neutral[70],
  },
  editFilter: {
    textColor: Colors.brand[30],
    deleteIconColor: Colors.brand[30],
  },
  heading: {
    ...baseTheme.heading,
    h4: Colors.neutral[0],
    base: {
      ...baseTheme.heading.base,
      color: Colors.neutral[0],
    },
  },
  code: {
    ...baseTheme.code,
    backgroundColor: Colors.neutral[95],
  },
  layout: {
    ...baseTheme.layout,
    stuffColor: Colors.neutral[75],
    stuffBorderColor: Colors.neutral[75],
    socialLink: Colors.neutral[30],
  },
  icons: {
    ...baseTheme.icons,
    editIcon: {
      normal: Colors.neutral[50],
      hover: Colors.neutral[30],
      active: Colors.neutral[40],
      border: Colors.neutral[70],
    },
    closeIcon: {
      normal: Colors.neutral[50],
      hover: Colors.neutral[30],
      active: Colors.neutral[40],
      border: Colors.neutral[70],
    },
    cancelIcon: Colors.neutral[0],
    autoIcon: Colors.neutral[0],
    fileIcon: Colors.neutral[0],
    clockIcon: Colors.neutral[0],
    arrowDownIcon: Colors.neutral[0],
    moonIcon: Colors.neutral[0],
    sunIcon: Colors.neutral[0],
    infoIcon: Colors.neutral[70],
    savedIcon: Colors.brand[30],
    github: {
      ...baseTheme.icons.github,
      hover: Colors.neutral[70],
      active: Colors.neutral[85],
    },
    discord: {
      ...baseTheme.icons.discord,
      normal: Colors.neutral[30],
    },
    producthunt: {
      ...baseTheme.icons.producthunt,
      normal: Colors.neutral[5],
    },
    menuIcon: Colors.brand[0],
    ftsIcon: {
      normal: Colors.neutral[50],
      active: Colors.brand[10],
    },
  },
  textArea: {
    ...baseTheme.textArea,
    borderColor: {
      ...baseTheme.textArea.borderColor,
      normal: Colors.neutral[70],
      hover: Colors.neutral[30],
      focus: Colors.neutral[0],
    },
  },
  clusterConfigForm: {
    ...baseTheme.clusterConfigForm,
    groupField: {
      backgroundColor: Colors.neutral[85],
    },
    fileInput: {
      color: Colors.neutral[0],
    },
  },
  acl: {
    table: {
      deleteIcon: Colors.neutral[50],
    },
    create: {
      radioButtons: {
        green: {
          normal: {
            background: Colors.neutral[90],
            text: Colors.neutral[50],
            border: Colors.neutral[80],
          },
          active: {
            background: Colors.green[50],
            text: Colors.neutral[0],
            border: Colors.green[50],
          },
          hover: {
            background: Colors.green[50],
            text: Colors.neutral[0],
            border: Colors.green[50],
          },
        },
        gray: {
          normal: {
            background: Colors.neutral[90],
            text: Colors.neutral[50],
            border: Colors.neutral[80],
          },
          active: {
            background: Colors.neutral[80],
            text: Colors.neutral[0],
            border: Colors.neutral[80],
          },
          hover: {
            background: Colors.neutral[85],
            text: Colors.neutral[0],
            border: Colors.neutral[85],
          },
        },
        red: {
          normal: {
            background: Colors.neutral[90],
            text: Colors.neutral[50],
            border: Colors.neutral[80],
          },
          active: {
            background: Colors.red[50],
            text: Colors.neutral[0],
            border: Colors.red[50],
          },
          hover: {
            background: Colors.red[50],
            text: Colors.neutral[0],
            border: Colors.red[50],
          },
        },
      },
    },
  },
  clusterColorPicker: {
    backgroundColor: Colors.clusterColorPicker,
    outline: Colors.brand[80],
    transparentCircle: {
      border: Colors.brand[60],
      cross: Colors.brand[30],
    },
  },
};
